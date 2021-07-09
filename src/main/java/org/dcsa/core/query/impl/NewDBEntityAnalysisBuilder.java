package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.*;
import org.dcsa.core.model.*;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.sql.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 1. Generates an EntityTree
 * 2. Creates a select query.
 * @param <T> Entity type
 */
@RequiredArgsConstructor
public class NewDBEntityAnalysisBuilder<T> implements DBEntityAnalysis.DBEntityAnalysisBuilder<T> {

    private final Class<T> entityType;

    private final Map<String, QueryField> jsonName2DbField = new HashMap<>();
    private final Map<String, QueryField> selectName2DbField = new HashMap<>();
    private final Map<String, QueryField> internalQueryName2DbField = new HashMap<>();
    private final Set<String> declaredButNotSelectable = new HashSet<>();
    private final Map<Class<?>, Set<String>> model2Aliases = new HashMap<>();
    private final Map<String, Class<?>> joinAlias2Class = new HashMap<>();
    private final LinkedHashMap<String, JoinDescriptor> joinAlias2Descriptor = new LinkedHashMap<>();

    private TableAndJoins tableAndJoins;
    private EntityTreeNode rootEntityTreeNode;
    private final List<QueryField> allSelectableFields = new LinkedList<>();
    private Map<EntityTreeNode, Table> entityTreeNode2Table = new HashMap<>();

    private boolean used = false;


    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> loadFieldsAndJoinsFromModel() {
        generateEntityTree();
        generateTables();
        generateJoins();
        generateQueryFields();
        return this;
    }

    public JoinDescriptor getJoinDescriptor(Class<?> modelClass) {
        Set<String> aliases = model2Aliases.get(modelClass);

        if (aliases.size() != 1) {
            if (aliases.isEmpty()) {
                throw new IllegalArgumentException("No join registered with model " + modelClass.getSimpleName());
            }
            String aliasNames = aliases.stream().sorted().collect(Collectors.joining(", "));
            throw new IllegalArgumentException("Multiple join descriptors match " + modelClass.getSimpleName()
                    + ": " + aliasNames);
        }
        return getJoinDescriptor(aliases.stream().findFirst().get());
    }

    public JoinDescriptor getJoinDescriptor(String aliasId) {
        return joinAlias2Descriptor.get(aliasId);
    }

    public Table getPrimaryModelTable() {
        return getTableAndJoins().getPrimaryTable();
    }

    public TableAndJoins getTableAndJoins() {
        if (tableAndJoins == null) {
            String tableName = ReflectUtility.getTableName(getPrimaryModelClass());
            Table primaryModelTable = Table.create(tableName);
            tableAndJoins = new TableAndJoins(primaryModelTable);
        }
        return tableAndJoins;
    }

    private void initJoinAliasTable() {
        // Ensure the join alias for the primary model is defined before adding any other aliases
        // as the code assumes this to be the case.
        Class<?> primaryModelClass = getPrimaryModelClass();
        String tableName = ReflectUtility.getTableName(primaryModelClass);
        // Predefine the "primary" alias (which is the one used in FROM)
        addNewAlias(primaryModelClass, tableName, null);
    }

    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> registerJoinDescriptor(JoinDescriptor joinDescriptor) {
        String rhsJoinAlias = joinDescriptor.getJoinAliasId();
        System.out.println(rhsJoinAlias);
        Class<?> rhsModel = joinDescriptor.getRHSModel();
        if (this.joinAlias2Class.isEmpty()) {
            initJoinAliasTable();
        }
        if (rhsModel == null) {
            rhsModel = Object.class;
        }
        addNewAlias(rhsModel, rhsJoinAlias, joinDescriptor);
        joinAlias2Descriptor.put(rhsJoinAlias, joinDescriptor);
        getTableAndJoins().addJoinDescriptor(joinDescriptor);
        return this;
    }

    public Class<?> getPrimaryModelClass() {
        PrimaryModel annotation = entityType.getAnnotation(PrimaryModel.class);
        if (annotation == null) {
            if (entityType.isAnnotationPresent(org.springframework.data.relational.core.mapping.Table.class)) {
                return entityType;
            }
            throw new IllegalArgumentException("Missing @PrimaryModel or @Table on class " + entityType.getSimpleName());
        }
        return annotation.value();
    }

    private void addNewAlias(Class<?> clazz, String joinAlias, JoinDescriptor joinDescriptor) {
        assert clazz == getPrimaryModelClass() || !joinAlias2Class.isEmpty();
        if (joinAlias2Class.putIfAbsent(joinAlias, clazz) != null) {
            if (joinDescriptor instanceof JoinedWithModelBasedJoinDescriptor) {
                String name = ReflectUtility.getTableName(clazz);
                if (name.equals(joinAlias)) {
                    throw new IllegalArgumentException("Invalid @JoinWithModel on " + entityType.getSimpleName() + ": "
                            + ": The join alias \"" + joinAlias + "\" for " + clazz.getSimpleName()
                            + " is already in use.  Note if you need to join the same table twice, you need to "
                            + " use lhsJoinAlias / rhsJoinAlias to avoid name clashes.");
                }
                throw new IllegalArgumentException("Invalid @JoinWithModel on " + entityType.getSimpleName()
                        + ": The join alias \"" + joinAlias + "\" for " + clazz.getSimpleName()
                        + " is already in use.  Please use unique names with each alias.");
            } else {
                throw new IllegalArgumentException("Invalid joinAlias \"" + joinAlias + "\": It is already in use!");
            }
        }
        this.model2Aliases.computeIfAbsent(clazz, c -> new HashSet<>()).add(joinAlias);
    }

    private void generateEntityTree() {
        rootEntityTreeNode = EntityTreeNode.of(entityType, "", null, null, null);
        loadModelDeep(entityType, rootEntityTreeNode, false);
    }

    private void loadModelDeep(Class<?> modelType, EntityTreeNode currentNode, boolean skipQueryFields) {
        System.out.println("New model: " + modelType.getSimpleName());
        loadJoinedWithModelAnnotationsDeep(modelType, currentNode);
        loadFieldsDeep(modelType, currentNode, skipQueryFields);
    }

    private void loadFieldsDeep(Class<?> modelType, EntityTreeNode currentNode, boolean skipQueryFields) {
        for (Field field : modelType.getDeclaredFields()) {
            System.out.println("New field: " + modelType.getSimpleName() +  " : " + field.getName() );
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
            MapEntity mapEntity = field.getAnnotation(MapEntity.class);
            if (foreignKey != null && mapEntity != null) {
                throw new IllegalStateException(modelType.getSimpleName() + "." + field.getName()
                        + " has both @MapEntity and @ForeignKey.  Please remove one of them.");
            }

            if (mapEntity != null) {
                EntityTreeNode mappedNode = currentNode.getChild(mapEntity.joinAlias());
                loadModelDeep(field.getType(), mappedNode, skipQueryFields);
            }

            if (foreignKey != null) {
                String intoFieldName = foreignKey.into();
                String fromFieldName = foreignKey.fromFieldName();
                Field intoField;
                Field fromField;
                if (intoFieldName.equals("") && fromFieldName.equals("")) {
                    throw new IllegalArgumentException("Invalid @ForeignKey on " + modelType.getSimpleName() + "."
                            + field.getName() + ": exactly one of \"into\" OR \"fromFieldName\" must be given (neither were given)");
                }
                if (!intoFieldName.equals("") && !fromFieldName.equals("")) {
                    throw new IllegalArgumentException("Invalid @ForeignKey on " + modelType.getSimpleName() + "."
                            + field.getName() + ": exactly one of \"into\" OR \"fromFieldName\" must be given (both were given)");
                }
                if (intoFieldName.equals("")) {
                    intoField = field;
                    try {
                        fromField = ReflectUtility.getDeclaredField(modelType, fromFieldName);
                    } catch (NoSuchFieldException e) {
                        throw new IllegalArgumentException("Invalid from field name");
                    }
                } else {
                    fromField = field;
                    try {
                        intoField = ReflectUtility.getDeclaredField(modelType, intoFieldName);
                    } catch (NoSuchFieldException e) {
                        throw new IllegalArgumentException("Invalid into field name");
                    }
                }
                Class<?> intoModelType = intoField.getType();

                Join.JoinType joinType = foreignKey.joinType();
                String intoJoinAlias = foreignKey.viaJoinAlias();
                EntityTreeNode intoNode = EntityTreeNode.of(intoModelType, intoJoinAlias, joinType, fromField.getName(), foreignKey.foreignFieldName());
                currentNode.addChild(intoNode);
                loadModelDeep(intoModelType, intoNode, skipQueryFields);
            }

            // @Transient check must come after @ForeignKey as @ForeignKey can be used on the model field
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }

            if (!skipQueryFields) {
                QField queryField = QField.of(field);
                currentNode.addQueryField(queryField);
            }
        }
    }

    private void loadJoinedWithModelAnnotationsDeep(Class<?> modelType, EntityTreeNode currentNode) {
        JoinedWithModel[] joinAnnotations = modelType.getAnnotationsByType(JoinedWithModel.class);

        for (JoinedWithModel joinAnnotation : joinAnnotations) {
            Join.JoinType joinType = joinAnnotation.joinType();
            Class<?> lhsModel = joinAnnotation.lhsModel();
            if (lhsModel == Object.class) {
                lhsModel = modelType;
            }

            String lhsJoinAlias = getAliasOrTableName(lhsModel, joinAnnotation.lhsJoinAlias());
            String lhsFieldName = joinAnnotation.lhsFieldName();

            Class<?> rhsModel = joinAnnotation.rhsModel();
            String rhsJoinAlias = getAliasOrTableName(rhsModel, joinAnnotation.rhsJoinAlias());
            String rhsFieldName = joinAnnotation.rhsFieldName();

            EntityTreeNode rhsNode = EntityTreeNode.of(rhsModel, rhsJoinAlias, joinType, lhsFieldName, rhsFieldName);
            if (lhsModel == modelType) {
                currentNode.addChild(rhsNode);
            } else {
                // Add to previously created rhsNode referenced by alias
                currentNode.getChild(lhsJoinAlias).addChild(rhsNode);
            }
            loadModelDeep(rhsModel, rhsNode, true);

            /* Fail-fast on unsupported joins */
            switch (joinType) {
                case JOIN:
                case LEFT_OUTER_JOIN:
                    break;
                case RIGHT_OUTER_JOIN:
                    throw new UnsupportedOperationException("Cannot generate query for " + modelType.getSimpleName()
                            + ": Please replace RIGHT JOINs with LEFT JOINs");
                default:
                    /* Remember to update JoinDescriptor.apply */
                    throw new IllegalArgumentException("Unsupported joinType: " + joinType.name() + " on " + modelType.getSimpleName());
            }
        }
    }

    private void generateTables() {
        generateTablesDeep(rootEntityTreeNode, "");
    }

    private void generateTablesDeep(EntityTreeNode currentNode, String prefix) {
        Table prefixedTable = getTableForModel(currentNode.getModelType(), currentNode.getAlias(), prefix);
        entityTreeNode2Table.putIfAbsent(currentNode, prefixedTable); // TODO: Exception

        String newPrefix = "";
        if (currentNode.getLhsFieldName() != null) {
            // Avoid "." which causes issues for SQL - we use "__" to make more distinct and less likely clash by accident.
            newPrefix = prefix + currentNode.getLhsFieldName() + "__";
        }

        for (EntityTreeNode childNode : currentNode.getChildren()) {
            generateTablesDeep(childNode, newPrefix);
        }
    }

    private void generateJoins() {
        generatePrefixedJoinsDeep(rootEntityTreeNode, "");
    }

    private void generatePrefixedJoinsDeep(EntityTreeNode currentNode, String prefix) {
        String newPrefix = "";
        if (currentNode.getLhsFieldName() != null) {
            // Avoid "." which causes issues for SQL - we use "__" to make more distinct and less likely clash by accident.
            newPrefix = prefix + currentNode.getLhsFieldName() + "__";
        }


        for (EntityTreeNode childNode : currentNode.getChildren()) {
            // Join current node (lhs) with child node (rhs)
            Table lhsTable = getTableFor(currentNode);
            String lhsFieldName = getColumnName(currentNode.getModelType(), childNode.getLhsFieldName(), "lhs");
            Column lhsColumn = Column.create(SqlIdentifier.unquoted(lhsFieldName), lhsTable);
            Table rhsTable = getTableFor(childNode);
            String rhsFieldName = getColumnName(childNode.getModelType(), childNode.getRhsFieldName(), "rhs");
            Column rhsColumn = Column.create(SqlIdentifier.unquoted(rhsFieldName), rhsTable);

            String dependentAlias = prefix + currentNode.getAlias();
            JoinDescriptor joinDescriptor = SimpleJoinDescriptor.of(childNode.getJoinType(), lhsColumn, rhsColumn, childNode.getModelType(), dependentAlias);
            registerJoinDescriptor(joinDescriptor);

            generatePrefixedJoinsDeep(childNode, newPrefix);
        }
    }

    private void generateQueryFields() {
        generatePrefixedQueryFieldsDeep(rootEntityTreeNode, "");
    }

    private void generatePrefixedQueryFieldsDeep(EntityTreeNode currentNode, String prefix) {
        Class<?> modelType = currentNode.getModelType();
        Table table = getTableFor(currentNode);

        for (QField qField : currentNode.getQueryFields()) {
            Field field = qField.getField();
            System.out.println(currentNode.getAlias() + field.getName());

            QueryField queryField = QueryFields.queryFieldFromFieldWithSelectPrefix(modelType, field, modelType, table, true, prefix);
            registerQueryField(queryField);
        }



        for (EntityTreeNode childNode : currentNode.getChildren()) {
            // TODO: the prefix should use the _into_ field name, as this is used when mapping result to an object.
            String newPrefix = prefix + childNode.getLhsFieldName() + ".";
            generatePrefixedQueryFieldsDeep(childNode, newPrefix);
        }
    }

    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> registerQueryField(QueryField queryField) {
        detectClash(jsonName2DbField, queryField.getJsonName(), queryField, "JSON key",
                "Probably one or both of them has a invalid @JsonProperty");
        if (queryField.isSelectable()) {
            Column column = Objects.requireNonNull(queryField.getSelectColumn(), "selectColumn");
            SqlIdentifier name = column.getName();
            if (column instanceof Aliased) {
                name = ((Aliased)column).getAlias();
            }
            this.allSelectableFields.add(queryField);
            detectClash(selectName2DbField, name.getReference(), queryField, "SelectColumn",
                    "This can happen with explicit @Column names in the combined model class. "
                            + "Note @Column is often unnecessary and removing it might be easier."
            );
        } else {
            Column selectableColumn = queryField.getSelectColumn();
            if (selectableColumn != null) {
                SqlIdentifier name = selectableColumn.getName();
                if (selectableColumn instanceof Aliased) {
                    name = ((Aliased)selectableColumn).getAlias();
                }
                declaredButNotSelectable.add(name.getReference());
            }
        }
        detectClash(internalQueryName2DbField, queryField.getInternalQueryColumn().toString(), queryField,
                "Internal Query name",
                "This should not happen (you ought to have gotten a conflict due to reused JoinAliases or Column names)"
        );
        return this;
    }

    private String getColumnName(Class<?> clazz, String fieldName, String annotationVariablePrefix) {
        try {
            return ReflectUtility.transformFromFieldNameToColumnName(
                    clazz,
                    fieldName,
                    /* We do not expect any ModelClass here, as it is not a field from the combined model */
                    false
            );
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Cannot resolve field " + fieldName + " on class "
                    + clazz.getSimpleName() + ".  It was defined in @JoinedWithModel on " + entityType.getSimpleName()
                    + " via " + annotationVariablePrefix + "Model and " + annotationVariablePrefix + "FieldName", e);
        }
    }

    private void detectClash(Map<String, QueryField> map, String key, QueryField value, String nameForKey, String hint) {
        QueryField clash = map.putIfAbsent(key, value);
        if (clash != null) {
            throw new IllegalArgumentException("Error in " + entityType.getSimpleName() + ": The fields "
                    + value.getJsonName() + " and "
                    + clash.getJsonName() + " both use \"" + key
                    + "\" as " + nameForKey + ". " + hint);
        }
    }

    private void verifyFieldsAndJoins() {
        Set<String> remainingAliases = new HashSet<>(joinAlias2Descriptor.keySet());
        Stack<String> checklist = new Stack<>();
        for (QueryField queryField : this.internalQueryName2DbField.values()) {
            String alias = queryField.getTableJoinAlias();
            if (remainingAliases.remove(alias)) {
                checklist.push(alias);
            }
        }

        while (!checklist.isEmpty()) {
            String alias = checklist.remove(checklist.size() - 1);
            JoinDescriptor joinDescriptor = joinAlias2Descriptor.get(alias);
            String dependentAlias = joinDescriptor.getDependentAlias();

            if (remainingAliases.remove(dependentAlias)) {
                checklist.push(dependentAlias);
            }
        }

        if (!remainingAliases.isEmpty()) {
            String leakedAlias = remainingAliases.iterator().next();
            JoinDescriptor joinDescriptor = joinAlias2Descriptor.get(leakedAlias);
            Class<?> rhsModel = joinDescriptor.getRHSModel();
            if (joinDescriptor instanceof JoinedWithModelBasedJoinDescriptor) {
                JoinedWithModelBasedJoinDescriptor jd = (JoinedWithModelBasedJoinDescriptor) joinDescriptor;
                throw new IllegalArgumentException("No fields defined for the table in @JoinedWithModel for rhsJoinAlias "
                        + leakedAlias + ", rhsModel: " + (rhsModel != Object.class ? rhsModel.getSimpleName() : "unset")
                        + ", rhsFieldName: " + jd.getJoinedWithModel().rhsFieldName()
                        + ". Hint:  Most likely the @JoinedWithModel is redundant or you might be missing a field."
                );
            } else {
                String modelRef = "No backing model)";
                if (rhsModel != null && rhsModel != Object.class) {
                    modelRef = rhsModel.getSimpleName();
                }
//                throw new IllegalArgumentException("Unnecessary join alias \"" + joinDescriptor.getJoinAliasId()
//                        + "\" from unknown source. Related Model: " + modelRef);
            }
        }

        // If there are clashes between these two, selectName2DbField decides.  Removing clashes will simplify
        // the logic in the lookup for selectable fields.
        this.declaredButNotSelectable.removeAll(this.selectName2DbField.keySet());
    }

    private String getAliasOrTableName(Class<?> model, String alias) {
        return getPrefixedTableName(model, alias, "");
    }

    private String getPrefixedTableName(Class<?> model, String alias, String prefix) {
        String tableName = ReflectUtility.getTableName(model);
        if (alias == null || alias.equals("")) {
            alias = tableName;
        }
        return prefix + alias;
    }

    private Table getTableForModel(Class<?> model, String alias, String prefix) {
        String tableName = ReflectUtility.getTableName(model);
        String prefixedTableName = getPrefixedTableName(model, alias, prefix);
        if (prefixedTableName.equals(tableName)) {
            return Table.create(SqlIdentifier.unquoted(tableName));
        }
        return Table.create(SqlIdentifier.unquoted(tableName)).as(SqlIdentifier.unquoted(prefixedTableName));
    }

    private Table getTableFor(EntityTreeNode entityTreeNode) {
        // TODO: Exception
        return entityTreeNode2Table.get(entityTreeNode);
    }

    private Table getTableForModel(Class<?> model, String alias) {
        String tableName = ReflectUtility.getTableName(model);
        if (alias == null || alias.equals("")) {
            alias = tableName;
        }
        if (alias.equals(tableName)) {
            return Table.create(SqlIdentifier.unquoted(tableName));
        }
        return Table.create(SqlIdentifier.unquoted(tableName)).as(SqlIdentifier.unquoted(alias));
    }

    private Table getTableFor(String alias) {
        JoinDescriptor joinDescriptor = getJoinDescriptor(alias);
        if (joinDescriptor == null) {
            return getPrimaryModelTable();
        }
        return joinDescriptor.getRHSTable();
    }

    private Table getTableFor(Class<?> model) {
        JoinDescriptor joinDescriptor = getJoinDescriptor(model);
        if (joinDescriptor == null) {
            return getPrimaryModelTable();
        }
        return joinDescriptor.getRHSTable();
    }

    private Class<?> getModelFor(String aliasId) {
        if (this.joinAlias2Class.isEmpty()) {
            initJoinAliasTable();
        }
        return joinAlias2Class.get(aliasId);
    }

    private Class<?> getModelFor(Table table) {
        return getModelFor(ReflectUtility.getAliasId(table));
    }

    public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onTable(String alias) {
        Class<?> model = getModelFor(alias);
        Table table = getTableFor(alias);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, table, model);
    }

    public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onTable(Class<?> model) {
        Table table = getTableFor(model);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, table, model);
    }

    public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onTable(Table table) {
        Class<?> model = getModelFor(table);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, table, model);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel) {
        Table rhsTable = getTableForModel(rhsModel, null);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsModel), rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel, String rhsJoinAlias) {
        Table rhsTable = getTableForModel(rhsModel, rhsJoinAlias);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsModel), rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel) {
        Table rhsTable = getTableForModel(rhsModel, null);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsJoinAlias), rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel, String rhsJoinAlias) {
        Table rhsTable = getTableForModel(rhsModel, rhsJoinAlias);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsJoinAlias), rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Table lhsTable, Table rhsTable) {
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, lhsTable, rhsTable, null);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Table lhsTable, Table rhsTable, Class<?> rhsModel) {
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, lhsTable, rhsTable, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> joinOn(Join.JoinType joinType, Column lhsColumn, Column rhsColumn) {
        return joinOnImpl(joinType, lhsColumn, rhsColumn, null);
    }

    DBEntityAnalysis.DBEntityAnalysisBuilder<T> joinOnImpl(Join.JoinType joinType, Column lhsColumn, Column rhsColumn, Class<?> rhsModel) {
        Table lhsTable = lhsColumn.getTable();
        if (lhsTable == null) {
            throw new IllegalArgumentException("lhsColumn must have a non-null getTable()");
        }
        if (rhsColumn.getTable() == null) {
            throw new IllegalArgumentException("rhsColumn must have a non-null getTable()");
        }
        String lhsAlias = ReflectUtility.getAliasId(lhsTable);
        return registerJoinDescriptor(SimpleJoinDescriptor.of(joinType, lhsColumn, rhsColumn, rhsModel, lhsAlias));
    }

    DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> joinOnThenImpl(Join.JoinType joinType, Column lhsColumn, Column rhsColumn, Class<?> rhsModel) {
        joinOnImpl(joinType, lhsColumn, rhsColumn, rhsModel);
        return DBEntityAnalysisWithTableBuilderImpl.of(this, Objects.requireNonNull(rhsColumn.getTable()), rhsModel);
    }

    public DBEntityAnalysis<T> build() {
        if (used) {
            throw new IllegalStateException("Already used");
        }
        this.verifyFieldsAndJoins();
        used = true;
        return new DefaultDBEntityAnalysis<>(
                Collections.unmodifiableMap(jsonName2DbField),
                Collections.unmodifiableMap(selectName2DbField),
                Collections.unmodifiableSet(declaredButNotSelectable),
                Collections.unmodifiableList(allSelectableFields),
                getTableAndJoins()
        );
    }

    @RequiredArgsConstructor(staticName = "of")
    private static class DBEntityAnalysisJoinBuilderImpl<T> implements DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> {
        private final NewDBEntityAnalysisBuilder<T> builder;
        private final Join.JoinType joinType;
        private final Table lhsTable;
        private final Table rhsTable;
        private final Class<?> rhsModel;

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> onEquals(String lhsColumnName, String rhsColumnName) {
            return onEqualsImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> onEquals(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName) {
            return onEqualsImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onEqualsThen(String lhsColumnName, String rhsColumnName) {
            return onEqualsThenImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onEqualsThen(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName) {
            return onEqualsThenImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        private <P> DBEntityAnalysis.DBEntityAnalysisBuilder<T> onEqualsImpl(P lhs, P rhs, BiFunction<P, Table, Column> toColumn) {
            Column lhsColumn = toColumn.apply(lhs, lhsTable);
            Column rhsColumn = toColumn.apply(rhs, rhsTable);
            return builder.joinOnImpl(joinType, lhsColumn, rhsColumn, rhsModel);
        }

        private <P> DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onEqualsThenImpl(P lhs, P rhs, BiFunction<P, Table, Column> toColumn) {
            Column lhsColumn = toColumn.apply(lhs, lhsTable);
            Column rhsColumn = toColumn.apply(rhs, rhsTable);
            return builder.joinOnThenImpl(joinType, lhsColumn, rhsColumn, rhsModel);
        }

        private <B> B onFieldEqualsImpl(String lhsFieldName, String rhsFieldName, BiFunction<String, String, B> impl) {
            Class<?> lhsModel = builder.getModelFor(lhsTable);
            String lhsColumnName;
            String rhsColumnName;
            if (lhsModel == null) {
                throw new UnsupportedOperationException("Cannot use field based join as the join is not based on a " +
                        "Model class (LHS side missing). Please use join/chainJoin with a Class parameter if you need this feature.");
            }
            if (rhsModel == null) {
                throw new UnsupportedOperationException("Cannot use field based join as the join is not based on a " +
                        "Model class (RHS side missing). Please use join/chainJoin with a Class parameter if you need this feature.");
            }
            try {
                lhsColumnName = ReflectUtility.transformFromFieldNameToColumnName(lhsModel, lhsFieldName,
                        builder.getPrimaryModelClass() == lhsModel
                );
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Unknown field \"" + lhsFieldName + "\" on model " + lhsModel.getSimpleName() + " (khs)");
            }
            try {
                rhsColumnName = ReflectUtility.transformFromFieldNameToColumnName(rhsModel, rhsFieldName, false);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("Unknown field \"" + rhsFieldName + "\" on model " + rhsModel.getSimpleName() + " (rhs)");
            }
            return impl.apply(lhsColumnName, rhsColumnName);
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> onFieldEquals(String lhsFieldName, String rhsFieldName) {
            return onFieldEqualsImpl(lhsFieldName, rhsFieldName, this::onEquals);
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> onFieldEqualsThen(String lhsFieldName, String rhsFieldName) {
            return onFieldEqualsImpl(lhsFieldName, rhsFieldName, this::onEqualsThen);
        }
    }

    @RequiredArgsConstructor(staticName = "of")
    private static class DBEntityAnalysisWithTableBuilderImpl<T> implements DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> {

        private final NewDBEntityAnalysisBuilder<T> builder;
        private final Table lhsTable;
        private final Class<?> lhsModel;

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel) {
            return chainJoin(Join.JoinType.JOIN, rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Class<?> rhsModel, String rhsJoinAlias) {
            return chainJoin(Join.JoinType.JOIN, rhsModel, rhsJoinAlias);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Table rhsTable) {
            return chainJoin(Join.JoinType.JOIN, rhsTable);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel) {
            return chainJoinImpl(joinType, builder.getTableForModel(rhsModel, null), rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel, String rhsJoinAlias) {
            return chainJoinImpl(joinType, builder.getTableForModel(rhsModel, rhsJoinAlias), rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Table rhsTable) {
            return chainJoinImpl(joinType, rhsTable, null);
        }

        private DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoinImpl(Join.JoinType joinType, Table rhsTable, Class<?> rhsModel) {
            return DBEntityAnalysisJoinBuilderImpl.of(builder, joinType, lhsTable, rhsTable, rhsModel);
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> registerQueryFieldFromField(String fieldName) {
            Field field;
            if (lhsModel == null) {
                throw new UnsupportedOperationException("Cannot use field based field registration as there is no model"
                        + " associated with the table");
            }
            try {
                field = ReflectUtility.getDeclaredField(lhsModel, fieldName);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("The model " + lhsModel.getSimpleName() + " does not have a field \""
                        + fieldName + "\"");
            }
            return builder.registerQueryField(QueryFields.queryFieldFromField(
                    builder.getPrimaryModelClass(),
                    field,
                    lhsModel,
                    lhsTable,
                    false
            ));
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> registerQueryField(SqlIdentifier columnName, String jsonName, Class<?> valueType) {
            Column column = Column.create(columnName, lhsTable);
            return builder.registerQueryField(QueryFields.nonSelectableQueryField(column, jsonName, valueType));
        }

        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> registerQueryFieldFromFieldThen(String fieldName) {
            registerQueryFieldFromField(fieldName);
            return this;
        }
        public DBEntityAnalysis.DBEntityAnalysisWithTableBuilder<T> registerQueryFieldThen(SqlIdentifier columnName, String jsonName, Class<?> valueType) {
            registerQueryField(columnName, jsonName, valueType);
            return this;
        }
    }
}
