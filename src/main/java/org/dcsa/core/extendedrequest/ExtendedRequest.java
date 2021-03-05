package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.Getter;
import lombok.Setter;
import org.dcsa.core.exception.GetException;
import org.dcsa.core.model.JoinedWithModel;
import org.dcsa.core.model.ModelClass;
import org.dcsa.core.model.PrimaryModel;
import org.dcsa.core.model.ViaJoinAlias;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.r2dbc.core.DatabaseClient;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * A class to handle extended requests. Extended requests is request requiring one of the following:
 * pagination, filtering, sorting or limiting of Collection results.
 * It converts between JSON names, POJO field names and Database column names using the modelClass
 * which is the POJO used as DAO. This is done by looking at @JsonProperty and @Column annotations on the modelClass.
 * This class contains 4 helper classes: Sort, Join, Filter and Pagination whom are specialized classes handling
 * different parts of the request.
 * The class manages everything from creating the SQL to extract the result to creating the query parameter for
 * paginated results.
 * All parameters can be configured in Application.yaml, default values are stored in the ExtendedParameter class
 * NB: Because if time limitations the current implementation simulates Key-Set based pagination but is in fact
 * offset-based pagination (OFFSET is used in the SQL queries)
 * @param <T> the type of the class modeled by this {@code Class}
 *  * object.
 */
public class ExtendedRequest<T> {

    public static final String PARAMETER_SPLIT = "&";
    public static final String CURSOR_SPLIT = "=";

    protected Sort<T> sort;
    protected Pagination<T> pagination;
    protected Join join = new Join();
    protected Filter<T> filter;
    @Getter
    @Setter
    protected boolean selectDistinct;

    private final ExtendedParameters extendedParameters;
    private final Class<T> modelClass;
    private Boolean isCursor = null;

    private final List<QueryField> allSelectableFields = new ArrayList<>();
    private final Map<String, QueryField> jsonName2DbField = new HashMap<>();
    private final Map<String, QueryField> internalQueryName2DbField = new HashMap<>();
    private final Map<String, QueryField> selectName2DbField = new HashMap<>();
    private final Set<String> declaredButNotSelectable = new HashSet<>();
    private final Map<Class<?>, List<String>> model2Aliases = new HashMap<>();
    private final Map<String, Class<?>> joinAlias2Class = new HashMap<>();
    private final LinkedHashMap<String, JoinDescriptor> joinAlias2Descriptor = new LinkedHashMap<>();

    private boolean joinsResolved = false;

    public ExtendedRequest(ExtendedParameters extendedParameters, Class<T> modelClass) {
        this.modelClass = modelClass;
        this.extendedParameters = extendedParameters;
        this.findAllTablesAndBuildJoins();
        this.loadAllFields();
        this.verifyFieldsAndJoins();
    }

    public Class<T> getModelClass() {
        return modelClass;
    }

    public Class<?> getPrimaryModelClass() {
        PrimaryModel annotation = modelClass.getAnnotation(PrimaryModel.class);
        if (annotation == null) {
            if (modelClass.isAnnotationPresent(Table.class)) {
                return modelClass;
            }
            throw new IllegalArgumentException("Missing @PrimaryModel or @Table on class " + modelClass.getSimpleName());
        }
        return annotation.value();
    }

    public ExtendedParameters getExtendedParameters() {
        return extendedParameters;
    }

    public boolean isCursor() {
        return isCursor != null && isCursor;
    }

    public void setNoCursor() {
        isCursor = false;
    }

    public void parseParameter(Map<String, String> params) {
        parseParameter(params, false);
    }

    public void parseParameter(Map<String, String> params, boolean fromCursor) {
        // Reset parameters
        resetParameters();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (!getExtendedParameters().getReservedParameters().contains(key)) {
                parseParameter(key, entry.getValue(), fromCursor);
            }
        }

        for (FilterItem item : filter.getFilters()) {
            this.markQueryFieldInUse(item.getQueryField());
        }
        finishedParsingParameters();
    }

    // For sub-classes to hook into this
    protected void finishedParsingParameters() {
        // Do nothing by default
    }

    public void resetParameters() {
        sort = new Sort<>(this, getExtendedParameters());
        pagination = new Pagination<>(this, getExtendedParameters());
        filter = new Filter<>(this, getExtendedParameters());
        join = new Join();
        for (JoinDescriptor descriptor : this.joinAlias2Descriptor.values()) {
            descriptor.setInUse(false);
        }
        selectDistinct = false;
        joinsResolved = false;
    }

    public Filter<T> getFilter() {
        return filter;
    }

    public Sort<T> getSort() {
        return sort;
    }

    public Pagination<T> getPagination() {
        return pagination;
    }

    public Join getJoin() {
        return join;
    }

    private void parseParameter(String key, String value, boolean fromCursor) {
        if (getExtendedParameters().getSortParameterName().equals(key)) {
            // Parse sorting
            getSort().parseSortParameter(value, fromCursor);
        } else if (getExtendedParameters().getPaginationPageSizeName().equals(key)) {
            // Parse limit
            getPagination().parseLimitParameter(value, fromCursor);
        } else if (getExtendedParameters().getPaginationCursorName().equals(key)) {
            // Parse cursor
            parseCursorParameter(value);
        } else if (getExtendedParameters().getIndexCursorName().equals(key) && fromCursor) {
            // Parse internal pagination cursor
            getPagination().parseInternalPaginationCursor(value);
        } else {
            getFilter().parseFilterParameter(key, value, fromCursor);
        }
    }

    private void parseCursorParameter(String cursorValue) {
        if (isCursor != null && !isCursor) {
            throw new GetException("Cannot use " + getExtendedParameters().getPaginationCursorName() + " parameter in combination with Sorting, Filtering or Limiting of the result!");
        }

        byte[] decodedCursor = Base64.getUrlDecoder().decode(cursorValue);

        // If encryption is used - decrypt the parameter
        String encryptionKey = getExtendedParameters().getEncryptionKey();
        if (encryptionKey != null) {
            decodedCursor = decrypt(encryptionKey, decodedCursor);
        }

        Map<String, String> params = convertToQueryStringToHashMap(new String(decodedCursor, StandardCharsets.UTF_8));
        parseParameter(params, true);
        isCursor = true;
    }

    private static Map<String, String> convertToQueryStringToHashMap(String source) {
        Map<String, String> data = new HashMap<>();
        final String[] arrParameters = source.split(PARAMETER_SPLIT);
        for (final String tempParameterString : arrParameters) {
            final String[] arrTempParameter = tempParameterString.split("=");
            final String parameterKey = arrTempParameter[0];
            if (arrTempParameter.length >= 2) {
                final String parameterValue = arrTempParameter[1];
                data.put(parameterKey, parameterValue);
            } else {
                data.put(parameterKey, "");
            }
        }
        return data;
    }

    public DatabaseClient.GenericExecuteSpec getCount(DatabaseClient databaseClient) {
        DatabaseClient.GenericExecuteSpec genericExecuteSpec = databaseClient.sql(this::getCountQuery);
        return setBinds(genericExecuteSpec);
    }

    public DatabaseClient.GenericExecuteSpec getFindAll(DatabaseClient databaseClient) {
        DatabaseClient.GenericExecuteSpec genericExecuteSpec = databaseClient.sql(this::getQuery);
        return setBinds(genericExecuteSpec);
    }

    private DatabaseClient.GenericExecuteSpec setBinds(DatabaseClient.GenericExecuteSpec genericExecuteSpec) {
        for (FilterItem filterItem : getFilter().getFilters()) {
            if (filterItem.doBind()) {
                genericExecuteSpec = genericExecuteSpec.bind(filterItem.getBindName(), filterItem.getQueryFieldValue());
            }
        }
        return genericExecuteSpec;
    }

    public void setQueryCount(Integer count) {
        getPagination().setTotal(count);
    }

    private void ensureJoinsAreResolved() {
        if (!joinsResolved) {
            /* Ensure selectable fields are always considered used */
            for (QueryField selectableField : allSelectableFields) {
                markQueryFieldInUse(selectableField);
            }
            for (JoinDescriptor joinDescriptor : joinAlias2Descriptor.values()) {
                if (joinDescriptor.isInUse()) {
                    joinDescriptor.apply(join);
                }
            }
            joinsResolved = true;
        }
    }

    public String getCountQuery() {
        StringBuilder sb = new StringBuilder(selectDistinct ? "SELECT DISTINCT COUNT(*) FROM ": "SELECT COUNT(*) FROM ");
        getTableName(sb);
        ensureJoinsAreResolved();
        getJoin().getJoinQueryString(sb);
        getFilter().getFilterQueryString(sb);
        return sb.toString();
    }

    /**
     * Called whenever a field is being used
     *
     * This is called when a field has been detected as in use for the coming query.
     * Unused fields can cause joins to be omitted.
     *
     * Note that any field registered via {@link #getFilter()} are automatically
     * registered via {@link #parseParameter(Map, boolean)} (etc.).  However, subclasses
     * that need special handling of some fields/Sort rules, etc. can call this to
     * register the fields as in used as needed.
     *
     * @param fieldInUse The DbField that has been used
     */
    protected void markQueryFieldInUse(QueryField fieldInUse) {
        String joinAlias = fieldInUse.getTableJoinAlias();
        while (joinAlias != null) {
            JoinDescriptor descriptor = this.joinAlias2Descriptor.get(joinAlias);
            if (descriptor == null || descriptor.isInUse()) {
                break;
            }
            descriptor.setInUse(true);
            joinAlias = descriptor.getDependentAlias();
        }
    }


    /**
     * Called during initialization once for each unique field that this ExtendedRequest *might* see/filter on.
     *
     * Subclasses can override {@link #loadAllFields()} to choose how fields are discovered and then call this
     * method to register them.  The fields are not required to be on the primary model class nor the "combined"
     * model class.  However, they must be unique in terms of java field name, their internal query name and
     * their select name for a given ExtendedRequest.
     *
     * Note this method resolve {@link ModelClass} and {@link ViaJoinAlias} annotations for resolving column
     * names.
     *
     * This is a useful short hand for creating a {@link QueryField} from a Java field and then calling
     * {@link #registerQueryField(QueryField)}.
     *
     * @param field The java field.
     * @param modelType The model containing the field.  Can be null in which case {@link #getModelClass()} is
     *                  used.  In general, selectable fields should always use null or {@link #getModelClass()}.
     *                  Implement special-cases at your own peril.
     * @param selectable If true, the field will be selectable - i.e. a field will need is value.  When false,
     *                   it can only be used in filters/order by (etc.).  This also avoids issues with
     *                   duplicated names for the SELECT clause as "non-selectable" fields do not reserve
     *                   a "SELECT name".
     */
    protected void registerField(Field field, Class<?> modelType, boolean selectable) {
        Class<?> modelClassForField;
        List<String> joinAliasesForModel;
        String joinAlias = null;
        QueryField queryField;
        ViaJoinAlias viaJoinAlias = field.getAnnotation(ViaJoinAlias.class);
        if (modelType == null) {
            modelType = modelClass;
        }
        modelClassForField = ReflectUtility.getFieldModelClass(modelType, field);

        /* Rewrite the combined model to the primary model class as that is what we used for defining
         * join aliases.
         */
        if (modelClassForField == modelClass) {
            modelClassForField = getPrimaryModelClass();
        }

        joinAliasesForModel = model2Aliases.get(modelClassForField);

        if (joinAliasesForModel == null) {
            throw new IllegalArgumentException("Missing Join for " + field.getName() + " on class "
                    + modelClass.getSimpleName() + ". There is no join for model "
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
                        + ": The alias is not declared in any @JoinWithModel for " + modelClass.getSimpleName());
            }
        } else if (joinAliasesForModel.size() == 1) {
            joinAlias = joinAliasesForModel.get(0);
        }
        if (joinAlias == null) {
            joinAlias = getTableName(modelClassForField);
            if (!joinAliasesForModel.contains(joinAlias)) {
                throw new IllegalArgumentException("Cannot compute automatic join alias for " + field.getName()
                        + " on class " + modelClassForField.getSimpleName()
                        + ", there are more than 1 option and the default name is not one of them.");
            }
        }
        queryField = QueryFields.queryFieldFromField(modelType, field, modelClassForField, joinAlias, selectable);
        registerQueryField(queryField);
    }

    /**
     * Called during initialization once for each unique field that this ExtendedRequest *might* see/filter on.
     *
     * Subclasses can override {@link #loadAllFields()} to choose how fields are discovered and then call this
     * method (or {@link #registerField(Field, Class, boolean)}) to register them. Field registered via this
     * method are <em>not</em> required to exist on any Java class provided they are not selectable
     * ({@link QueryField#isSelectable()}.  However, they are required to have unique Json names, internal query
     * names and (if selectable) select column names.
     *
     * Note that
     *
     * @param queryField Register the query field.  If {@link QueryField#isSelectable()} returns true, then
     *                   the field will be extracted from the database query (using
     *                   {@link QueryField#getSelectColumnName()} as name).
     */
    protected void registerQueryField(QueryField queryField) {
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

    }

    /**
     * Called during initialization to load and register all fields.
     *
     * Subclasses can hook into this via {@link #loadFieldsFromSubclass()} if they want to load
     * additional fields that are not covered by the model or joins.  Every field should be
     * registered via {@link #registerField(Field, Class, boolean)}.
     */
    protected void loadAllFields() {
        loadFieldFromModelClass();
        loadFilterFieldsFromJoinedWithModelDeclaredJoins();
        loadFieldsFromSubclass();
    }

    protected void loadFieldFromModelClass() {
        ReflectUtility.visitAllFields(
                modelClass,
                field -> !field.isAnnotationPresent(Transient.class),
                field -> this.registerField(field, null, true)
        );
    }

    protected void loadFilterFieldsFromJoinedWithModelDeclaredJoins() {
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
                    throw new IllegalArgumentException("Invalid @JoinedWithModel on " + modelClass.getSimpleName()
                            + ": The rhsModel " + filterModelClass.getSimpleName() + " does not have a field called "
                            + fieldName + ", but it is listed under filterFields."
                    );
                }
                this.registerField(field, filterModelClass, false);
            }
        }
    }

    protected void loadFieldsFromSubclass() {
        /* do nothing; it is for subclasses to hook into the field registration */
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

    private void detectClash(Map<String, QueryField> map, String key, QueryField value, String nameForKey, String hint) {
        QueryField clash = map.putIfAbsent(key, value);
        if (clash != null) {
            throw new IllegalArgumentException("Error in " + modelClass.getSimpleName() + ": The fields "
                    + value.getJsonName() + " and "
                    + clash.getJsonName() + " both use \"" + key
                    + "\" as " + nameForKey + ". " + hint);
        }
    }

    private void addNewAlias(Class<?> clazz, String joinAlias, JoinDescriptor joinDescriptor) {
        if (joinAlias2Class.putIfAbsent(joinAlias, clazz) != null) {
            if (joinDescriptor instanceof JoinedWithModelBasedJoinDescriptor) {
                String name = getTableName(clazz);
                if (name.equals(joinAlias)) {
                    throw new IllegalArgumentException("Invalid @JoinWithModel on " + modelClass.getSimpleName() + ": "
                            + ": The join alias \"" + joinAlias + "\" for " + clazz.getSimpleName()
                            + " is already in use.  Note if you need to join the same table twice, you need to "
                            + " use lhsJoinAlias / rhsJoinAlias to avoid name clashes.");
                }
                throw new IllegalArgumentException("Invalid @JoinWithModel on " + modelClass.getSimpleName()
                        + ": The join alias \"" + joinAlias + "\" for " + clazz.getSimpleName()
                        + " is already in use.  Please use unique names with each alias.");
            } else {
                throw new IllegalArgumentException("Invalid joinAlias \"" + joinAlias + "\": It is already in use!");
            }
        }
        this.model2Aliases.computeIfAbsent(clazz, c -> new ArrayList<>()).add(joinAlias);
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
                    + clazz.getSimpleName() + ".  It was defined in @JoinedWithModel on " + modelClass.getSimpleName()
                    + " via " + annotationVariablePrefix + "Model and " + annotationVariablePrefix + "FieldName", e);
        }
    }

    protected void findAllTablesAndBuildJoins() {
        JoinedWithModel[] joinAnnotations = modelClass.getAnnotationsByType(JoinedWithModel.class);
        Class<?> primaryModelClass = getPrimaryModelClass();
        String tableName = this.getTableName(primaryModelClass);
        // Predefine the "primary" alias (which is the one used in FROM)
        addNewAlias(primaryModelClass, tableName, null);

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
            String rhsTableName = getTableName(rhsModel);
            String rhsColumnName = getColumnName(rhsModel, rhsFieldName, "rhs");

            if (!lhsJoinAlias.equals("")) {
                Class<?> lhsClassFromAlias = joinAlias2Class.get(lhsJoinAlias);
                if (lhsClassFromAlias == null) {
                    throw new IllegalArgumentException("Invalid @JoinWithModel on " + modelClass.getSimpleName()
                            + ": The lhsJoinAlias must use an earlier table name/alias (alias " + lhsJoinAlias + ")");
                }

                if (lhsModel == Object.class) {
                    lhsModel = lhsClassFromAlias;
                } else if (lhsClassFromAlias != lhsModel) {
                    throw new IllegalArgumentException("Invalid @JoinWithModel on " + modelClass.getSimpleName()
                            + ": The lhsJoinAlias and lhsModel are both defined but they do not agree on the type - "
                            + lhsClassFromAlias.getSimpleName() + " (via alias " + lhsJoinAlias + " ) != " + lhsModel);
                }

            } else if (lhsModel == Object.class) {
                lhsModel = primaryModelClass;
                lhsJoinAlias = tableName;
            } else {
                lhsJoinAlias = getTableName(lhsModel);
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
                case RIGHT_OUTER_JOIN:
                    break;
                default:
                    /* Remember to update JoinDescriptor.apply */
                    throw new IllegalArgumentException("Unsupported joinType: " + joinType.name());
            }
        }
    }

    protected void registerJoinDescriptor(JoinDescriptor joinDescriptor) {
        String rhsJoinAlias = joinDescriptor.getJoinAlias();
        if (joinDescriptor instanceof JoinedWithModelBasedJoinDescriptor) {
            JoinedWithModelBasedJoinDescriptor j = (JoinedWithModelBasedJoinDescriptor)joinDescriptor;
            JoinedWithModel joinedWithModel = j.getJoinedWithModel();
            addNewAlias(joinedWithModel.rhsModel(), rhsJoinAlias, joinDescriptor);
        } else {
            addNewAlias(Object.class, rhsJoinAlias, joinDescriptor);
        }
        joinAlias2Descriptor.put(rhsJoinAlias, joinDescriptor);
    }

    public void getTableFields(StringBuilder sb) {
        boolean first = true;
        for (QueryField queryField : this.allSelectableFields) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            markQueryFieldInUse(queryField);
            sb.append(queryField.getQueryInternalName())
                    /* Use quotes to preserve the case; which we need when resolving the column later */
                    .append(" AS \"").append(queryField.getSelectColumnName()).append("\"");
        }
    }

    public String getQuery() {
        StringBuilder sb = new StringBuilder(selectDistinct ? "SELECT DISTINCT " : "SELECT ");
        getTableFields(sb);
        sb.append(" FROM ");
        getTableName(sb);
        ensureJoinsAreResolved();
        getJoin().getJoinQueryString(sb);
        getFilter().getFilterQueryString(sb);
        getSort().getSortQueryString(sb);
        getPagination().getOffsetQueryString(sb);
        getPagination().getLimitQueryString(sb);
        return sb.toString();
    }

    public String getTableName(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        getTableName(clazz, sb);
        return sb.toString();
    }

    public void getTableName(StringBuilder sb) {
        getTableName(modelClass, sb);
    }

    public void getTableName(Class<?> clazz, StringBuilder sb) {
        PrimaryModel primaryModel = clazz.getAnnotation(PrimaryModel.class);
        Table table;
        if (primaryModel != null) {
            /* If we have a @PrimaryModel then we always use the @Table from referenced model */
            table = primaryModel.value().getAnnotation(Table.class);
        } else {
            table = clazz.getAnnotation(Table.class);
        }
        if (table == null) {
            throw new GetException("@Table not defined on class:" + clazz.getSimpleName());
        }
        sb.append(table.value());
    }

    public boolean ignoreUnknownProperties() {
        JsonIgnoreProperties jsonIgnoreProperties = modelClass.getAnnotation(JsonIgnoreProperties.class);
        return jsonIgnoreProperties != null && jsonIgnoreProperties.ignoreUnknown();
    }

    public T getModelClassInstance(Row row, RowMetadata meta) {
        try {
            Constructor<T> constructor = modelClass.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new GetException("Error when creating a new instance of: " + modelClass.getSimpleName());
        }
    }

    /*
     * DO NOT USE THIS CIPHER FOR ANYTHING IMPORTANT (we are not in asseco-reactive-api-code)
     * -> Reason: https://en.wikipedia.org/wiki/Block_cipher_mode_of_operation#Electronic_codebook_(ECB)
     *
     * In this case, we are using it as a mild deterrent for having people not fiddle with our cursor argument
     * because we intend to change the implementation at a later date.  Someone breaking it can at best change
     * which "page" they are on in a search and as such it has no real value for them to break the encryption
     * (and we lose very little if they do).
     *
     * If you are looking for a:
     *  * good encryption algorithm for keeping things *SECRET*, then AES/GCM/PKCS5PADDING is a much better choice
     *    but requires an IV (initialization vector), which we are too lazy to manage here (given the lack of value
     *    in what we are protecting).
     *  * good algorithm for keeping things *UNCHANGED* (or detect manipulation), then look for a HMAC a la
     *    HmacSHA512.
     *
     * Though, please consider if you are really on the right track.  Sending encrypted values to the client that
     * it is not supposed to understand is rarely a good idea.
     *
     * DO NOT COPY PASTE THIS WITHOUT UNDERSTANDING YOUR OWN USE CASE.  The /ECB/ part is insecure for almost
     * anything you want to keep a secret.
     */
    private static final String TRANSFORMATION = "AES/ECB/PKCS5PADDING"; /* !INSECURE! */

    private byte[] encrypt(String key, byte[] text) {
        try {
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            return cipher.doFinal(text);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException exception) {
            throw new GetException("Error creating encryption algorithm:" + exception.getMessage());
        }
    }

    private byte[] decrypt(String key, byte[] text) {
        try {
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            return cipher.doFinal(text);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException exception) {
            throw new GetException("Error creating encryption algorithm:" + exception.getMessage());
        }
    }

    public void insertHeaders(ServerHttpResponse response, ServerHttpRequest request) {
        HttpHeaders headers = response.getHeaders();
        StringBuilder exposeHeaders = new StringBuilder();

        String uri = getURI(request);
        addPaginationHeaders(exposeHeaders, headers, uri);

        // Expose headers to frontEnd
        response.getHeaders().add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders.toString());
    }

    protected void addPaginationHeaders(StringBuilder exposeHeaders, HttpHeaders headers, String uri) {
        if (getPagination().getLimit() != null) {
            addPaginationHeader(uri, headers, getExtendedParameters().getPaginationCurrentPageName(), Pagination.PageRequest.CURRENT, exposeHeaders);
            addPaginationHeader(uri, headers, getExtendedParameters().getPaginationFirstPageName(), Pagination.PageRequest.FIRST, exposeHeaders);
            addPaginationHeader(uri, headers, getExtendedParameters().getPaginationPreviousPageName(), Pagination.PageRequest.PREVIOUS, exposeHeaders);
            addPaginationHeader(uri, headers, getExtendedParameters().getPaginationNextPageName(), Pagination.PageRequest.NEXT, exposeHeaders);
            addPaginationHeader(uri, headers, getExtendedParameters().getPaginationLastPageName(), Pagination.PageRequest.LAST, exposeHeaders);
        } else {
            addPaginationHeader(uri, headers, getExtendedParameters().getPaginationCurrentPageName(), null, exposeHeaders);
        }
    }

    protected void addPaginationHeader(String uri, HttpHeaders headers, String headerName, Pagination.PageRequest page, StringBuilder exposeHeaders) {
        String pageHeader = getHeaderPageCursor(page);
        if (pageHeader != null) {
            headers.add(headerName, uri + (pageHeader.isEmpty() ? "" : "?" + pageHeader));
            exposeHeaders.append(headerName).append(',');
        }
    }

    protected String getURI(ServerHttpRequest request) {
        return request.getURI().getScheme() + "://" + request.getURI().getRawAuthority() + request.getURI().getRawPath();
    }

    private String getHeaderPageCursor(Pagination.PageRequest page) {
        StringBuilder sb = new StringBuilder();
        if (page != null && !getPagination().encodePagination(sb, page)) {
            return null;
        }
        getSort().encodeSort(sb);
        getFilter().encodeFilter(sb);
        getPagination().encodeLimit(sb);
        if (page == null) {
            return sb.toString();
        }
        byte[] parameters = sb.toString().getBytes(StandardCharsets.UTF_8);

        // If encryption is used - encrypt the parameter string
        String encryptionKey = getExtendedParameters().getEncryptionKey();
        if (encryptionKey != null) {
            parameters = encrypt(encryptionKey, parameters);
        }
        return getExtendedParameters().getPaginationCursorName() + CURSOR_SPLIT + Base64.getUrlEncoder().encodeToString(parameters);
    }

    /** A method to convert a JSON name to a field. The modelClass of this ExtendedRequest will be used
     * @param jsonName the JSON name to convert
     * @return the field name corresponding to the JSON name provided
     * @throws NoSuchFieldException if the JSON name is not found
     */
    public String transformFromJsonNameToFieldName(String jsonName) throws NoSuchFieldException {
        // Verify that the field exists on the model class and transform it from JSON-name to FieldName
        return ReflectUtility.transformFromJsonNameToFieldName(modelClass, jsonName);
    }

    private static QueryField getFieldFromTable(Map<String, QueryField> table, String key) throws NoSuchFieldException {
        QueryField field = table.get(key);
        if (field == null) {
            throw new NoSuchFieldException(key);
        }
        return field;
    }

    public QueryField getQueryFieldFromJsonName(String jsonName) throws NoSuchFieldException {
        return getFieldFromTable(jsonName2DbField, jsonName);
    }

    protected QueryField getQueryFieldFromInternalQueryName(String internalQueryName) throws NoSuchFieldException {
        return getFieldFromTable(internalQueryName2DbField, internalQueryName);
    }

    public QueryField getQueryFieldFromSelectName(String selectName) throws NoSuchFieldException {
        try {
            return getFieldFromTable(selectName2DbField, selectName);
        } catch (NoSuchFieldException e) {
            if (declaredButNotSelectable.contains(selectName)) {
                throw new IllegalArgumentException("Invalid " + selectName + ": The field is marked as non-selectable");
            }
            throw e;
        }
    }

    /** A method to return all fieldNames corresponding to the type specified. The class used is specified by clazz
     * @param clazz the class to find all fieldNames of type *type*
     * @param type the type of which to find fieldNames
     * @return a list of fieldNames of type *type* found in the class specified
     */
    protected String[] getFieldNamesOfType(Class<?> clazz, Class<?> type) {
        if (clazz != null) {
            return ReflectUtility.getFieldNamesOfType(clazz, type);
        } else {
            return ReflectUtility.getFieldNamesOfType(modelClass, type);
        }
    }

}
