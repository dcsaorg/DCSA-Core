package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.*;
import org.dcsa.core.model.ForeignKey;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.MapEntity;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.sql.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 1. Generates an EntityTree
 * 2. Creates a select query.
 * @param <T> Entity type
 */
@RequiredArgsConstructor
public class DefaultDBEntityAnalysisBuilder<T> extends AbstractDBEntityAnalysisBuilder<T> implements DBEntityAnalysis.DBEntityAnalysisBuilder<T> {

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
    private final Map<EntityTreeNode, Table> entityTreeNode2Table = new HashMap<>();

    private boolean used = false;


    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> loadFieldsAndJoinsFromModel() {
        generateEntityTree();
        generateTables();
        generateJoins();
        generateQueryFields();
        return this;
    }

    public JoinDescriptor getJoinDescriptor(Class<?> modelClass) {
        Set<String> aliases;
        if (this.joinAlias2Class.isEmpty()) {
            initJoinAliasTable();
        }
        aliases = model2Aliases.get(modelClass);


        if (aliases == null || aliases.size() != 1) {
            if (aliases == null || aliases.isEmpty()) {
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
        return entityType;
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
        rootEntityTreeNode = EntityTreeNode.of(null, entityType, "", null, null, null);
        loadModelDeep(entityType, rootEntityTreeNode, false);
    }

    private void loadModelDeep(Class<?> modelType, EntityTreeNode currentNode, boolean skipQueryFields) {
        loadJoinedWithModelAnnotationsDeep(modelType, currentNode);
        loadFieldsDeep(modelType, currentNode, skipQueryFields);
    }

    private void loadFieldsDeep(Class<?> modelType, EntityTreeNode currentNode, boolean skipQueryFields) {
        Set<String> seenFields = new HashSet<>();
        ReflectUtility.visitAllFields(modelType,
                field -> !seenFields.contains(field.getName()) && !Modifier.isStatic(field.getModifiers()),
                field -> {
                    seenFields.add(field.getName());

                    ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
                    MapEntity mapEntity = field.getAnnotation(MapEntity.class);
                    if (foreignKey != null && mapEntity != null) {
                        throw new IllegalStateException(modelType.getSimpleName() + "." + field.getName()
                                + " has both @MapEntity and @ForeignKey.  Please remove one of them.");
                    }

                    if (mapEntity != null) {
                        String alias = getAliasOrTableName(field.getType(), mapEntity.joinAlias());
                        EntityTreeNode mappedNode = currentNode.getChild(alias);
                        mappedNode.setSelectName(ReflectUtility.transformFromFieldNameToJsonName(field));
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
                            intoFieldName = field.getName();
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
                        EntityTreeNode intoNode = EntityTreeNode.of(
                                currentNode,
                                intoModelType, intoJoinAlias, joinType,
                                fromField.getName(), foreignKey.foreignFieldName());
                        intoNode.setSelectName(intoFieldName);
                        currentNode.addChild(intoNode);
                        loadModelDeep(intoModelType, intoNode, skipQueryFields);
                    }

                    // @Transient check must come after @ForeignKey as @ForeignKey can be used on the model field
                    if (field.isAnnotationPresent(Transient.class)) {
                        return;
                    }

                    if (!skipQueryFields) {
                        currentNode.addQueryField(QField.of(field, false));
                    }
                }
        );
    }

    private void loadJoinedWithModelAnnotationsDeep(Class<?> modelType, EntityTreeNode currentNode) {
        JoinedWithModel[] joinAnnotations = modelType.getAnnotationsByType(JoinedWithModel.class);

        Set<String> mapEntityJoinAliases = new HashSet<>();
        ReflectUtility.visitAllFields(modelType,
                field -> field.isAnnotationPresent(MapEntity.class),
                field -> {
                    MapEntity mapEntity = field.getAnnotation(MapEntity.class);
                    String joinAlias = getAliasOrTableName(field.getType(), mapEntity.joinAlias());
                    if (!mapEntityJoinAliases.add(joinAlias)) {
                        throw new IllegalArgumentException("Join alias " + joinAlias + " used twice for different @MapEntity fields");
                    }
        });

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

            EntityTreeNode rhsNode = EntityTreeNode.of(currentNode, rhsModel, rhsJoinAlias, joinType, lhsFieldName, rhsFieldName);
            if (lhsModel == modelType) {
                currentNode.addChild(rhsNode);
            } else {
                // Add to previously created rhsNode referenced by alias
                currentNode.getChild(lhsJoinAlias).addChild(rhsNode);
            }
            boolean noMapEntityAnnotation = !mapEntityJoinAliases.contains(rhsJoinAlias);
            loadModelDeep(rhsModel, rhsNode, noMapEntityAnnotation);
            checkJoinType(joinType, modelType);
            for (String fieldName : joinAnnotation.filterFields()) {
                Field field;
                try {
                    field = ReflectUtility.getDeclaredField(rhsModel, fieldName);
                } catch (NoSuchFieldException e) {
                    throw new IllegalArgumentException("Invalid @JoinedWithModel on " + entityType.getSimpleName()
                            + ": The rhsModel " + rhsModel.getSimpleName() + " does not have a field called "
                            + fieldName + ", but it is listed under filterFields."
                    );
                }
                rhsNode.addQueryField(QField.of(field, true));
            }
        }
    }

    private void generateTables() {
        generateTablesDeep(rootEntityTreeNode, "");
    }

    private void generateTablesDeep(EntityTreeNode currentNode, String prefix) {
        Table prefixedTable = getTableForModel(currentNode.getModelType(), currentNode.getAlias(), prefix);
        if (entityTreeNode2Table.putIfAbsent(currentNode, prefixedTable) != null) {
            throw new IllegalArgumentException("Cannot insert EntityTreeNode twice! (Got prefix: " + prefixedTable + ")");
        }

        String newPrefix = "";
        if (currentNode.getModelType() != entityType) {
            // Avoid "." which causes issues for SQL - we use "__" to make more distinct and less likely clash by accident.
            newPrefix = prefixedTable.getReferenceName().getReference() + "__";
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
        // Avoid "." which causes issues for SQL - we use "__" to make more distinct and less likely clash by accident.
        Table lhsTable = getTableFor(currentNode);
        if (currentNode.getModelType() != entityType) {
            newPrefix = lhsTable.getReferenceName().getReference() + "__";
        }

        for (EntityTreeNode childNode : currentNode.getChildren()) {
            // Join current node (lhs) with child node (rhs)
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
        generatePrefixedQueryFieldsDeep(rootEntityTreeNode);
    }

    private void generatePrefixedQueryFieldsDeep(EntityTreeNode currentNode) {
        Table table = getTableFor(currentNode);
        String prefix = currentNode.getSelectNamePrefix();

        for (QField qField : currentNode.getQueryFields()) {
            Field field = qField.getField();

            QueryField queryField;
            if (qField.isFilterField()) {
                queryField = QueryFields.queryFieldFromFieldWithSelectPrefix(field, table, true, prefix);
            } else {
                queryField = QueryFields.queryFieldFromFieldWithSelectPrefix(field, table, true, prefix);
            }
            registerQueryField(queryField);
        }

        for (EntityTreeNode childNode : currentNode.getChildren()) {
            EntityTreeNode parentModel = childNode.getParentModelNode();
            if (parentModel == null) {
                throw new IllegalStateException();
            }
            String newPrefix = parentModel.getSelectNamePrefix();
            if (childNode.getSelectName() != null) {
                if (newPrefix.equals("")) {
                    newPrefix = childNode.getSelectName() + ".";
                } else {
                    newPrefix = newPrefix + childNode.getSelectName() + ".";
                }
            }
            if (newPrefix.contains("..")) {
                throw new IllegalStateException();
            }
            childNode.setSelectNamePrefix(newPrefix);
            generatePrefixedQueryFieldsDeep(childNode);
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

    @Override
    protected String getColumnName(Class<?> clazz, String fieldName, String annotationVariablePrefix) {
        try {
            return ReflectUtility.transformFromFieldNameToColumnName(
                    clazz,
                    fieldName
            );
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException("Cannot resolve field " + fieldName + " on class "
                    + clazz.getSimpleName() + ".  It was defined in @JoinedWithModel or via @ForeignKey on " + entityType.getSimpleName()
                    + " via " + annotationVariablePrefix + "Model and " + annotationVariablePrefix + "FieldName", e);
        }
    }

    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> registerQueryFieldAlias(String jsonName, String jsonNameAlias) {
        QueryField queryField = jsonName2DbField.get(jsonName);
        if (queryField == null) {
            throw new IllegalArgumentException("No QueryField with jsonName " + jsonName + " exists");
        }
        detectClash(jsonName2DbField, jsonNameAlias, queryField, "JSON key (alias)",
                "The alias clashes with an existing field or alias");
        return this;
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
                String modelRef = "No backing model";
                if (rhsModel != null && rhsModel != Object.class) {
                    modelRef = rhsModel.getSimpleName();
                }

                throw new IllegalArgumentException("Unnecessary join alias \"" + joinDescriptor.getJoinAliasId()
                        + "\" from unknown source. Related Model: " + modelRef);
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
        Table table = entityTreeNode2Table.get(entityTreeNode);
        if (table == null) {
            throw new IllegalArgumentException("No table for the given EntityTreeNode");
        }
        return table;
    }

    @Override
    protected Table getTableForModel(Class<?> model, String alias) {
        String tableName = ReflectUtility.getTableName(model);
        if (alias == null || alias.equals("")) {
            alias = tableName;
        }
        if (alias.equals(tableName)) {
            return Table.create(SqlIdentifier.unquoted(tableName));
        }
        return Table.create(SqlIdentifier.unquoted(tableName)).as(SqlIdentifier.unquoted(alias));
    }

    @Override
    protected Class<?> getModelFor(String aliasId) {
        if (this.joinAlias2Class.isEmpty()) {
            initJoinAliasTable();
        }
        return joinAlias2Class.get(aliasId);
    }

    @Override
    protected Class<?> getModelFor(Table table) {
        return getModelFor(ReflectUtility.getAliasId(table));
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
}
