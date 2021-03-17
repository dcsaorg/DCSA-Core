package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.*;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.PrimaryModel;
import org.dcsa.core.model.ViaJoinAlias;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Join;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiFunction;
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
        JoinDescriptor joinDescriptor = joinAlias2Descriptor.get(aliasId);
        if (joinDescriptor == null) {
            throw new IllegalArgumentException("Unknown alias " + aliasId);
        }
        return joinDescriptor;
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
        if (this.joinAlias2Class.isEmpty()) {
            initJoinAliasTable();
        }
        if (joinDescriptor instanceof JoinedWithModelBasedJoinDescriptor) {
            JoinedWithModelBasedJoinDescriptor j = (JoinedWithModelBasedJoinDescriptor)joinDescriptor;
            JoinedWithModel joinedWithModel = j.getJoinedWithModel();
            addNewAlias(joinedWithModel.rhsModel(), rhsJoinAlias, joinDescriptor);
        } else {
            addNewAlias(Object.class, rhsJoinAlias, joinDescriptor);
        }
        joinAlias2Descriptor.put(rhsJoinAlias, joinDescriptor);
        getTableAndJoins().addJoinDescriptor(joinDescriptor);
        return this;
    }

    private void addNewAlias(Class<?> clazz, String joinAlias, JoinDescriptor joinDescriptor) {
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
        queryField = QueryFields.queryFieldFromField(modelType, field, modelClassForField, joinAlias, selectable);
        registerQueryField(queryField);
    }

    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> registerQueryField(QueryField queryField) {
        detectClash(jsonName2DbField, queryField.getJsonName(), queryField, "JSON key",
                "Probably one or both of them has a invalid @JsonProperty");
        if (queryField.isSelectable()) {
            String columnName = Objects.requireNonNull(queryField.getSelectColumnName(), "selectColumnName");
            this.allSelectableFields.add(queryField);
            detectClash(selectName2DbField, columnName, queryField, "SelectColumn",
                    "This can happen with explicit @Column names in the combined model class. "
                            + "Note @Column is often unnecessary and removing it might be easier."
            );
        } else {
            String selectableColumn = queryField.getSelectColumnName();
            if (selectableColumn != null) {
                declaredButNotSelectable.add(selectableColumn);
            }
        }
        detectClash(internalQueryName2DbField, queryField.getQueryInternalName(), queryField,
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
            if (joinDescriptor instanceof JoinedWithModelBasedJoinDescriptor) {
                JoinedWithModelBasedJoinDescriptor jd = (JoinedWithModelBasedJoinDescriptor)joinDescriptor;
                Class<?> rhsModel = jd.getJoinedWithModel().rhsModel();
                throw new IllegalArgumentException("No fields defined for the table in @JoinedWithModel for rhsJoinAlias "
                        + leakedAlias + ", rhsModel: " + (rhsModel != Object.class ? rhsModel.getSimpleName() : "unset")
                        + ", rhsFieldName: " + jd.getJoinedWithModel().rhsFieldName()
                        + ". Hint:  Most likely the @JoinedWithModel is redundant or you might be missing a field."
                );
            } else {
                throw new IllegalArgumentException("Unnecessary join alias \"" + joinDescriptor.getJoinAliasId()
                        + "\" from unknown source");
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
        Class<?> clazz = joinAlias2Class.get(aliasId);
        if (clazz == null) {
            throw new IllegalArgumentException("The alias/table " + aliasId + " is not backed by a Java model");
        }
        return clazz;
    }

    private Class<?> getModelFor(Table table) {
        return getModelFor(ReflectUtility.getAliasId(table));
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel) {
        Table rhsTable = getTableForModel(rhsModel, null);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsModel), rhsTable);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Class<?> lhsModel, Class<?> rhsModel, String rhsJoinAlias) {
        Table rhsTable = getTableForModel(rhsModel, rhsJoinAlias);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsModel), rhsTable);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel) {
        Table rhsTable = getTableForModel(rhsModel, null);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsJoinAlias), rhsTable);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, String lhsJoinAlias, Class<?> rhsModel, String rhsJoinAlias) {
        Table rhsTable = getTableForModel(rhsModel, rhsJoinAlias);
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, getTableFor(lhsJoinAlias), rhsTable);
    }

    public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> join(Join.JoinType joinType, Table lhsJoinAlias, Table rhsModel) {
        return DBEntityAnalysisJoinBuilderImpl.of(this, joinType, lhsJoinAlias, rhsModel);
    }

    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> joinOn(Join.JoinType joinType, Column lhsColumn, Column rhsColumn) {
        Table lhsTable = lhsColumn.getTable();
        if (lhsTable == null) {
            throw new IllegalArgumentException("lhsColumn must have a non-null getTable()");
        }
        if (rhsColumn.getTable() == null) {
            throw new IllegalArgumentException("rhsColumn must have a non-null getTable()");
        }
        String dependentAlias = ReflectUtility.getAliasId(lhsTable);
        return registerJoinDescriptor(SimpleJoinDescriptor.of(joinType, lhsColumn, rhsColumn, dependentAlias));
    }

    DBEntityAnalysis.DBEntityAnalysisChainJoinBuilder<T> joinOnThen(Join.JoinType joinType, Column lhsColumn, Column rhsColumn) {
        joinOn(joinType, lhsColumn, rhsColumn);
        return DBEntityAnalysisChainJoinBuilderImpl.of(this, Objects.requireNonNull(rhsColumn.getTable()));
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

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> onEquals(String lhsColumnName, String rhsColumnName) {
            return onEqualsImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        public DBEntityAnalysis.DBEntityAnalysisBuilder<T> onEquals(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName) {
            return onEqualsImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        public DBEntityAnalysis.DBEntityAnalysisChainJoinBuilder<T> onEqualsThen(String lhsColumnName, String rhsColumnName) {
            return onEqualsThenImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        public DBEntityAnalysis.DBEntityAnalysisChainJoinBuilder<T> onEqualsThen(SqlIdentifier lhsColumnName, SqlIdentifier rhsColumnName) {
            return onEqualsThenImpl(lhsColumnName, rhsColumnName, Column::create);
        }

        private <P> DBEntityAnalysis.DBEntityAnalysisBuilder<T> onEqualsImpl(P lhs, P rhs, BiFunction<P, Table, Column> toColumn) {
            Column lhsColumn = toColumn.apply(lhs, lhsTable);
            Column rhsColumn = toColumn.apply(rhs, rhsTable);
            return builder.joinOn(joinType, lhsColumn, rhsColumn);
        }

        private <P> DBEntityAnalysis.DBEntityAnalysisChainJoinBuilder<T> onEqualsThenImpl(P lhs, P rhs, BiFunction<P, Table, Column> toColumn) {
            Column lhsColumn = toColumn.apply(lhs, lhsTable);
            Column rhsColumn = toColumn.apply(rhs, rhsTable);
            return builder.joinOnThen(joinType, lhsColumn, rhsColumn);
        }

        private <B> B onFieldEqualsImpl(String lhsFieldName, String rhsFieldName, BiFunction<String, String, B> impl) {
            Class<?> lhsModel = builder.getModelFor(lhsTable);
            Class<?> rhsModel = builder.getModelFor(rhsTable);
            String lhsColumnName;
            String rhsColumnName;
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
            return onFieldEqualsImpl(lhsFieldName, rhsFieldName, this::onFieldEquals);
        }

        public DBEntityAnalysis.DBEntityAnalysisChainJoinBuilder<T> onFieldEqualsThen(String lhsFieldName, String rhsFieldName) {
            return onFieldEqualsImpl(lhsFieldName, rhsFieldName, this::onFieldEqualsThen);
        }

    }

    @RequiredArgsConstructor(staticName = "of")
    private static class DBEntityAnalysisChainJoinBuilderImpl<T> implements DBEntityAnalysis.DBEntityAnalysisChainJoinBuilder<T> {

        private final DefaultDBEntityAnalysisBuilder<T> builder;
        private final Table lhsTable;

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
            return chainJoin(joinType, builder.getTableForModel(rhsModel, null));
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Class<?> rhsModel, String rhsJoinAlias) {
            return chainJoin(joinType, builder.getTableForModel(rhsModel, rhsJoinAlias));
        }

        public DBEntityAnalysis.DBEntityAnalysisJoinBuilder<T> chainJoin(Join.JoinType joinType, Table rhsTable) {
            return builder.join(joinType, lhsTable, rhsTable);
        }
    }
}
