package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.JoinDescriptor;
import org.dcsa.core.extendedrequest.JoinedWithModelBasedJoinDescriptor;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.extendedrequest.QueryFields;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.PrimaryModel;
import org.dcsa.core.model.ViaJoinAlias;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.sql.SqlIdentifier;

import java.lang.reflect.Field;
import java.util.*;

@RequiredArgsConstructor
public class DefaultDBEntityAnalysisBuilder<T> implements DBEntityAnalysis.DBEntityAnalysisBuilder<T> {

    private final Class<T> entityType;

    private final Map<String, QueryField> jsonName2DbField = new HashMap<>();
    private final Map<String, QueryField> selectName2DbField = new HashMap<>();
    private final List<QueryField> allSelectableFields = new ArrayList<>();
    private final Map<String, QueryField> internalQueryName2DbField = new HashMap<>();
    private final Set<String> declaredButNotSelectable = new HashSet<>();
    private final Map<Class<?>, List<String>> model2Aliases = new HashMap<>();
    private final Map<String, Class<?>> joinAlias2Class = new HashMap<>();
    private final LinkedHashMap<String, JoinDescriptor> joinAlias2Descriptor = new LinkedHashMap<>();

    private boolean used = false;


    public DBEntityAnalysis.DBEntityAnalysisBuilder<T> loadFieldsAndJoinsFromModel() {
        loadJoinsFromAnnotationsOnEntityClass();
        loadFieldFromModelClass();
        loadFilterFieldsFromJoinedWithModelDeclaredJoins();
        return this;
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
        String rhsJoinAlias = joinDescriptor.getJoinAlias();
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
        this.model2Aliases.computeIfAbsent(clazz, c -> new ArrayList<>()).add(joinAlias);
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
            if (entityType.isAnnotationPresent(Table.class)) {
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
            String joinCondition;

            Class<?> lhsModel = joinAnnotation.lhsModel();
            String lhsJoinAlias = joinAnnotation.lhsJoinAlias();
            String lhsFieldName = joinAnnotation.lhsFieldName();
            String lhsColumnName;

            Class<?> rhsModel = joinAnnotation.rhsModel();
            String rhsJoinAlias = joinAnnotation.rhsJoinAlias();
            String rhsFieldName = joinAnnotation.rhsFieldName();
            String rhsTableName = ReflectUtility.getTableName(rhsModel);
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

            if (rhsJoinAlias.equals("")) {
                rhsJoinAlias = rhsTableName;
            }

            lhsColumnName = getColumnName(lhsModel, lhsFieldName, "lhs");

            joinCondition = " ON " + lhsJoinAlias + "." + lhsColumnName + "=" + rhsJoinAlias + "." + rhsColumnName;

            JoinDescriptor joinDescriptor = JoinedWithModelBasedJoinDescriptor.of(joinType, rhsTableName, rhsJoinAlias,
                    joinCondition, lhsJoinAlias, joinAnnotation, rhsModel);
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
        List<String> joinAliasesForModel;
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
            joinAlias = joinAliasesForModel.get(0);
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
                throw new IllegalArgumentException("Unnecessary join alias \"" + joinDescriptor.getJoinAlias()
                        + "\" from unknown source");
            }
        }

        // If there are clashes between these two, selectName2DbField decides.  Removing clashes will simplify
        // the logic in the lookup for selectable fields.
        this.declaredButNotSelectable.removeAll(this.selectName2DbField.keySet());
    }

    public DBEntityAnalysis<T> build() {
        if (used) {
            throw new IllegalStateException("Already used");
        }
        this.verifyFieldsAndJoins();
        used = true;
        String tableName = ReflectUtility.getTableName(getPrimaryModelClass());
        return new DefaultDBEntityAnalysis<>(
                Collections.unmodifiableMap(jsonName2DbField),
                Collections.unmodifiableMap(selectName2DbField),
                Collections.unmodifiableSet(declaredButNotSelectable),
                Collections.unmodifiableList(allSelectableFields),
                org.springframework.data.relational.core.sql.Table.create(tableName).as(SqlIdentifier.quoted(tableName)),
                Collections.unmodifiableMap(joinAlias2Descriptor)
        );
    }
}
