package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.dcsa.core.exception.GetException;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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
    protected Join join;
    protected Filter<T> filter;

    private final ExtendedParameters extendedParameters;
    private final Class<T> modelClass;
    private Boolean isCursor = null;

    public ExtendedRequest(ExtendedParameters extendedParameters, Class<T> modelClass) {
        this.modelClass = modelClass;
        this.extendedParameters = extendedParameters;
    }

    public Class<T> getModelClass() {
        return modelClass;
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
    }

    public void resetParameters() {
        sort = new Sort<>(this, getExtendedParameters());
        pagination = new Pagination<>(this, getExtendedParameters());
        join = new Join();
        filter = new Filter<T>(this, getExtendedParameters());
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
            try {
                // Parse filtering
                getFilter().parseFilterParameter(key, value, fromCursor);
            } catch (NoSuchFieldException noSuchFieldException) {
                if (!doJoin(key, value, fromCursor)) {
                    throw new GetException("Filter parameter not recognized: " + key);
                }
            }
        }
    }

    protected boolean doJoin(String parameter, String value, boolean fromCursor) {
        return false;
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
        DatabaseClient.GenericExecuteSpec genericExecuteSpec = databaseClient.execute(this::getCountQuery);
        return setBinds(genericExecuteSpec);
    }
    public DatabaseClient.GenericExecuteSpec getFindAll(DatabaseClient databaseClient) {
        DatabaseClient.GenericExecuteSpec genericExecuteSpec = databaseClient.execute(this::getQuery);
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

    public String getCountQuery() {
        StringBuilder sb = new StringBuilder("select count(*) from ");
        getTableName(sb);
        getJoin().getJoinQueryString(sb);
        getFilter().getFilterQueryString(sb);
        return sb.toString();
    }

    public void getTableFields(StringBuilder sb) {
        sb.append("*");
    }

    public String getQuery() {
        StringBuilder sb = new StringBuilder("select ");
        getTableFields(sb);
        sb.append(" from ");
        getTableName(sb);
        getJoin().getJoinQueryString(sb);
        getFilter().getFilterQueryString(sb);
        getSort().getSortQueryString(sb);
        getPagination().getOffsetQueryString(sb);
        getPagination().getLimitQueryString(sb);
        return sb.toString();
    }

    public String getTableName() {
        StringBuilder sb = new StringBuilder();
        getTableName(sb);
        return sb.toString();
    }

    public void getTableName(StringBuilder sb) {
        getTableName(modelClass, sb);
    }

    public void getTableName(Class<?> clazz, StringBuilder sb) {
        Table table = clazz.getAnnotation(Table.class);
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

    private static final String TRANSFORMATION = "AES/ECB/PKCS5PADDING";

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


    /** A method to convert a JSON name to a field name using a specified class.
     * @param clazz the class to use. If not provided the modelClass for this ExtendedRequest will be used
     * @param jsonName the JSON name to convert
     * @return the field name corresponding to the JSON name provided, specified on the class
     * @throws NoSuchFieldException if the JSON name is not found
     */
    protected String transformFromJsonNameToFieldName(Class<?> clazz, String jsonName) throws NoSuchFieldException {
        if (clazz != null) {
            return ReflectUtility.transformFromJsonNameToFieldName(clazz, jsonName);
        } else {
            return transformFromJsonNameToFieldName(jsonName);
        }
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

    /** A method to convert a field name to a database column name using a specified class.
     * @param clazz the class to use. If not provided the modelClass for this ExtendedRequest will be used
     * @param fieldName the field name to convert
     * @return the column name corresponding to the field name provided, specified on the class
     * @throws NoSuchFieldException if the field name is not found
     */
    public String transformFromFieldNameToColumnName(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        if (clazz != null) {
            return ReflectUtility.transformFromFieldNameToColumnName(clazz, fieldName);
        } else {
            return transformFromFieldNameToColumnName(fieldName);
        }
    }

    /** A method to convert a field name to a database column name. The modelClass of this ExtendedRequest will be used
     * @param fieldName the field name to convert
     * @return the column name corresponding to the field name
     * @throws NoSuchFieldException if the field name is not found
     */
    public String transformFromFieldNameToColumnName(String fieldName) throws NoSuchFieldException {
        return ReflectUtility.transformFromFieldNameToColumnName(modelClass, fieldName);
    }

    /** A method to convert a field name to a JSON name using a specified class.
     * @param clazz the class to use. If not provided the modelClass for this ExtendedRequest will be used
     * @param fieldName the field name to convert
     * @return the JSON name corresponding to the field name provided, specified on the class
     * @throws NoSuchFieldException if the field name is not found
     */
    public String transformFromFieldNameToJsonName(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        if (clazz != null) {
            return ReflectUtility.transformFromFieldNameToJsonName(clazz, fieldName);
        } else {
            return transformFromFieldNameToJsonName(fieldName);
        }
    }

    /** A method to convert a field name to a JSON name. The modelClass of this ExtendedRequest will be used
     * @param fieldName the field name to convert
     * @return the JSON name corresponding to the field name
     * @throws NoSuchFieldException if the field name is not found
     */
    protected String transformFromFieldNameToJsonName(String fieldName) throws NoSuchFieldException {
        return ReflectUtility.transformFromFieldNameToJsonName(modelClass, fieldName);
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
            return getFieldNamesOfType(type);
        }
    }

    /** A method to return all fieldNames corresponding to the type specified. The modelClass of this ExtendedRequest will be used
     * @param type the type of which to find fieldNames
     * @return a list of fieldNames of type *type* on modelClass
     */
    public String[] getFieldNamesOfType(Class<?> type) {
        return ReflectUtility.getFieldNamesOfType(modelClass, type);
    }

    /** A method to return the type of the field by the name of fieldName on the class clazz
     * @param clazz the class to find the fieldName on
     * @param fieldName the field name of which to find the type
     * @return the class correspoinding to the type of fieldName
     * @throws NoSuchFieldException if the fieldName is not found
     */
    protected Class<?> getFieldType(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        if (clazz != null) {
            return ReflectUtility.getFieldType(clazz, fieldName);
        } else {
            return getFieldType(fieldName);
        }
    }

    /** A method to return the type of the field by the name of fieldName on modelClass
     * @param fieldName the field name of which to find the type
     * @return the class correspoinding to the type of fieldName
     * @throws NoSuchFieldException if the fieldName is not found
     */
    protected Class<?> getFieldType(String fieldName) throws NoSuchFieldException {
        return ReflectUtility.getFieldType(modelClass, fieldName);
    }
}
