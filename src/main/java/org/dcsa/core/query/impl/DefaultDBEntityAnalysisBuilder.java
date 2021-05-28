package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.*;
import org.dcsa.core.model.*;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.sql.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DefaultDBEntityAnalysisBuilder<T> implements DBEntityAnalysis.DBEntityAnalysisBuilder<T> {

    private final Class<T> entityType;

    private final Map<String, QueryField> jsonName2DbField = new HashMap<>();
    private final Map<String, QueryField> selectName2DbField = new HashMap<>();
    private final List<QueryField> allSelectableFields = new ArrayList<>();
    private final Map<String, QueryField> internalQueryName2DbField = new HashMap<>();
    private final Set<String> declaredButNotSelectable = new HashSet<>();
    private final Map<Class<?>, Set<String>> model2Aliases = new HashMap<>();
    private final Map<String, Class<?>> joinAlias2Class = new HashMap<>();
    private final LinkedHashMap<String, JoinDescriptor> joinAlias2Descriptor = new LinkedHashMap<>();
    private TableAndJoins tableAndJoins;

    private boolean used = false;


    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> loadFieldsAndJoinsFromModel() {
        loadForeignKeysFromEntityClass();
        loadJoinsFromAnnotationsOnEntityClass();
        loadFieldFromModelClass();
        loadFilterFieldsFromJoinedWithModelDeclaredJoins();
        return this;
    }

    public TableAndJoins getTableAndJoins() {
        if (tableAndJoins == null) {
            String tableName = ReflectUtility.getTableName(getPrimaryModelClass());
            Table primaryModelTable = Table.create(tableName);
            tableAndJoins = new TableAndJoins(primaryModelTable);
        }
        return tableAndJoins;
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

    private void loadFieldFromModelClass() {
        ReflectUtility.visitAllFields(
                entityType,
                field -> !field.isAnnotationPresent(Transient.class),
                field -> this.registerField(field, null, true)
        );
    }

    private void loadForeignKeysFromEntityClass() {
        if (joinAlias2Class.isEmpty()) {
            initJoinAliasTable();
        }

        loadJoinsAndFieldsAndForeignKeysDeep(entityType, model -> model.isAssignableFrom(entityType), "", "");
    }

    private void loadJoinsAndFieldsAndForeignKeysDeep(Class<?> modelType, Predicate<Class<?>> skipFieldRegistration, String prefix, String joinAlias) {
        if (joinAlias.equals("")) {
            joinAlias = ReflectUtility.getTableName(modelType);
        }
        Table table = getTableForModel(modelType, joinAlias);

        for (Field field : modelType.getDeclaredFields()) {
            if (field.isAnnotationPresent(Transient.class)) {
                continue;
            }

            if (field.isAnnotationPresent(ForeignKey.class)) {
                ForeignKey foreignKey = field.getAnnotation(ForeignKey.class);
                String intoFieldName = foreignKey.into();
                Field intoField;
                try {
                    intoField = ReflectUtility.getDeclaredField(modelType, intoFieldName);
                } catch (NoSuchFieldException e) {
                    throw new IllegalArgumentException("Invalid into");
                }
                Class<?> intoModelType = intoField.getType();
                String intoJoinAlias = foreignKey.viaJoinAlias();

                // Join descriptor
                String lhsColumnName = getColumnName(modelType, field.getName(), "lhs");
                Column lhsColumn = Column.create(SqlIdentifier.unquoted(lhsColumnName), table);
                String rhsColumnName = getColumnName(intoModelType, foreignKey.foreignFieldName(), "rhs");
                Table rhsTable = getTableForModel(intoModelType, intoJoinAlias);
                Column rhsColumn = Column.create(SqlIdentifier.unquoted(rhsColumnName), rhsTable);

                SimpleJoinDescriptor joinDescriptor = SimpleJoinDescriptor.of(Join.JoinType.JOIN, lhsColumn, rhsColumn, intoModelType, joinAlias);
                registerJoinDescriptor(joinDescriptor);

                // load fields recursively with new prefix
                String newPrefix = prefix + ReflectUtility.transformFromFieldNameToJsonName(intoField) + ".";
                loadJoinsAndFieldsAndForeignKeysDeep(intoField.getType(), skipFieldRegistration, newPrefix, intoJoinAlias);
            }

            if (skipFieldRegistration != null && skipFieldRegistration.test(modelType)) {
                continue;
            }

            QueryField queryField = QueryFields.queryFieldFromFieldWithSelectPrefix(entityType, field, modelType, table, true, prefix);
            registerQueryField(queryField);
        }

        Class<?> superClass = modelType.getSuperclass();
        if (superClass != Object.class) {
            loadJoinsAndFieldsAndForeignKeysDeep(superClass, skipFieldRegistration, prefix, joinAlias);
        }
    }

    private void loadFilterFieldsFromJoinedWithModelDeclaredJoins() {
        for (JoinDescriptor joinDescriptor : this.joinAlias2Descriptor.values()) {
            if (!(joinDescriptor instanceof JoinedWithModelBasedJoinDescriptor)) {
                continue;
            }
            JoinedWithModelBasedJoinDescriptor joinedWithModelBasedJoinDescriptor = (JoinedWithModelBasedJoinDescriptor)joinDescriptor;
            JoinedWithModel joinAnnotation = joinedWithModelBasedJoinDescriptor.getJoinedWithModel();
            String[] filterFields = joinAnnotation.filterFields();
            Class<?> filterModelClass = joinedWithModelBasedJoinDescriptor.getModelClass();
            for (String fieldName : filterFields) {
                Field field;
                try {
                    field = ReflectUtility.getDeclaredField(filterModelClass, fieldName);
                } catch (NoSuchFieldException e) {
                    throw new IllegalArgumentException("Invalid @JoinedWithModel on " + entityType.getSimpleName()
                            + ": The rhsModel " + filterModelClass.getSimpleName() + " does not have a field called "
                            + fieldName + ", but it is listed under filterFields."
                    );
                }
                this.registerField(field, filterModelClass, false);
            }
        }
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

    private void loadJoinsFromAnnotationsOnEntityClass() {
        JoinedWithModel[] joinAnnotations = entityType.getAnnotationsByType(JoinedWithModel.class);
        Class<?> primaryModelClass = getPrimaryModelClass();
        String tableName = ReflectUtility.getTableName(primaryModelClass);

        if (joinAlias2Class.isEmpty()) {
            initJoinAliasTable();
        }

        for (JoinedWithModel joinAnnotation : joinAnnotations) {
            org.springframework.data.relational.core.sql.Join.JoinType joinType = joinAnnotation.joinType();
            Class<?> lhsModel = joinAnnotation.lhsModel();
            String lhsJoinAlias = joinAnnotation.lhsJoinAlias();
            String lhsFieldName = joinAnnotation.lhsFieldName();
            String lhsColumnName;
            Table lhsTable;

            Class<?> rhsModel = joinAnnotation.rhsModel();
            String rhsJoinAlias = joinAnnotation.rhsJoinAlias();
            String rhsFieldName = joinAnnotation.rhsFieldName();
            String rhsColumnName = getColumnName(rhsModel, rhsFieldName, "rhs");

            if (!lhsJoinAlias.equals("")) {
                Class<?> lhsClassFromAlias = joinAlias2Class.get(lhsJoinAlias);
                if (lhsClassFromAlias == null) {
                    throw new IllegalArgumentException("Invalid @JoinWithModel on " + entityType.getSimpleName()
                            + ": The lhsJoinAlias must use an earlier table name/alias (alias " + lhsJoinAlias + ")");
                }

                if (lhsModel == Object.class) {
                    lhsModel = lhsClassFromAlias;
                } else if (lhsClassFromAlias != lhsModel) {
                    throw new IllegalArgumentException("Invalid @JoinWithModel on " + entityType.getSimpleName()
                            + ": The lhsJoinAlias and lhsModel are both defined but they do not agree on the type - "
                            + lhsClassFromAlias.getSimpleName() + " (via alias " + lhsJoinAlias + " ) != " + lhsModel);
                }
            } else if (lhsModel == Object.class) {
                lhsModel = primaryModelClass;
                lhsJoinAlias = tableName;
            } else {
                lhsJoinAlias = ReflectUtility.getTableName(lhsModel);
            }

            JoinDescriptor lhsJoinDescriptor = joinAlias2Descriptor.get(lhsJoinAlias);
            if (lhsJoinDescriptor != null) {
                lhsTable = lhsJoinDescriptor.getRHSTable();
            } else {
                lhsTable = getPrimaryModelTable();
            }

            lhsColumnName = getColumnName(lhsModel, lhsFieldName, "lhs");
            Column lhsColumn = Column.create(SqlIdentifier.unquoted(lhsColumnName), lhsTable);
            Table rhsTable = getTableForModel(rhsModel, rhsJoinAlias);
            Column rhsColumn = Column.create(SqlIdentifier.unquoted(rhsColumnName), rhsTable);

            JoinDescriptor joinDescriptor = JoinedWithModelBasedJoinDescriptor.of(joinType, lhsColumn, rhsColumn,
                    lhsJoinAlias, joinAnnotation, rhsModel);
            registerJoinDescriptor(joinDescriptor);

            /* Fail-fast on unsupported joins */
            switch (joinType) {
                case JOIN:
                case LEFT_OUTER_JOIN:
                    break;
                case RIGHT_OUTER_JOIN:
                    throw new UnsupportedOperationException("Cannot generate query for " + entityType.getSimpleName()
                            + ": Please replace RIGHT JOINs with LEFT JOINs");
                default:
                    /* Remember to update JoinDescriptor.apply */
                    throw new IllegalArgumentException("Unsupported joinType: " + joinType.name() + " on " + entityType.getSimpleName());
            }
        }
    }

    private void registerField(Field field, Class<?> modelType, boolean selectable) {
        Class<?> modelClassForField;
        Set<String> joinAliasesForModel;
        String joinAlias = null;
        QueryField queryField;
        ViaJoinAlias viaJoinAlias = field.getAnnotation(ViaJoinAlias.class);
        if (modelType == null) {
            modelType = entityType;
        }
        modelClassForField = ReflectUtility.getFieldModelClass(modelType, field);

        /* Rewrite the combined model to the primary model class as that is what we used for defining
         * join aliases.
         */
        if (modelClassForField == entityType) {
            modelClassForField = getPrimaryModelClass();
        }

        joinAliasesForModel = model2Aliases.get(modelClassForField);

        if (joinAliasesForModel == null) {
            throw new IllegalArgumentException("Missing Join for " + field.getName() + " on class "
                    + entityType.getSimpleName() + ". There is no join for model "
                    + modelClassForField.getSimpleName()
            );
        }

        if (viaJoinAlias != null) {
            joinAlias = viaJoinAlias.value();
            if (joinAlias.equals("")) {
                throw new IllegalStateException("Cannot have empty alias in @ViaJoinAlias annotation on field "
                        + field.getName() + " on class " + modelClassForField.getSimpleName());
            }
            if (!joinAliasesForModel.contains(joinAlias)) {
                throw new IllegalArgumentException("Invalid join alias defined in @ViaJoinAlias annotation on field "
                        + field.getName() + " on class " + modelClassForField.getSimpleName()
                        + ": The alias is not declared in any @JoinWithModel for " + entityType.getSimpleName());
            }
        } else if (joinAliasesForModel.size() == 1) {
            joinAlias = joinAliasesForModel.stream().findFirst().get();
        }
        if (joinAlias == null) {
            joinAlias = ReflectUtility.getTableName(modelClassForField);
            if (!joinAliasesForModel.contains(joinAlias)) {
                throw new IllegalArgumentException("Cannot compute automatic join alias for " + field.getName()
                        + " on class " + modelClassForField.getSimpleName()
                        + ", there are more than 1 option and the default name is not one of them.");
            }
        }
        JoinDescriptor descriptor = joinAlias2Descriptor.get(joinAlias);
        Table fieldTable;
        if (descriptor != null) {
            fieldTable = descriptor.getRHSTable();
        } else {
            fieldTable = getPrimaryModelTable();
        }
        queryField = QueryFields.queryFieldFromField(modelType, field, modelClassForField, fieldTable, selectable);
        registerQueryField(queryField);
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
                throw new IllegalArgumentException("Unnecessary join alias \"" + joinDescriptor.getJoinAliasId()
                        + "\" from unknown source. Related Model: " + modelRef);
            }
        }

        // If there are clashes between these two, selectName2DbField decides.  Removing clashes will simplify
        // the logic in the lookup for selectable fields.
        this.declaredButNotSelectable.removeAll(this.selectName2DbField.keySet());
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
        private final DefaultDBEntityAnalysisBuilder<T> builder;
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

        private final DefaultDBEntityAnalysisBuilder<T> builder;
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
