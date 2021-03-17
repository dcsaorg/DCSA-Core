package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.dcsa.core.exception.GetException;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.ReflectUtility;
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
@RequiredArgsConstructor
public class ExtendedRequest<T> {

    public static final String PARAMETER_SPLIT = "&";
    public static final String CURSOR_SPLIT = "=";


    private final ExtendedParameters extendedParameters;
    private final Class<T> modelClass;

    protected Sort<T> sort;
    protected Pagination<T> pagination;
    protected Filter<T> filter;
    @Getter
    @Setter
    protected boolean selectDistinct;

    private Boolean isCursor = null;
    private final Set<String> joinAliasInUse = new HashSet<>();
    @Getter
    private DBEntityAnalysis<T> dbEntityAnalysis;

    private boolean joinsResolved = false;

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
        selectDistinct = false;
        joinsResolved = false;
        dbEntityAnalysis = this.prepareDBEntityAnalysis().build();
        joinAliasInUse.clear();
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
            for (QueryField selectableField : dbEntityAnalysis.getAllSelectableFields()) {
                markQueryFieldInUse(selectableField);
            }
            joinsResolved = true;
        }
    }

    public String getCountQuery() {
        StringBuilder sb = new StringBuilder(selectDistinct ? "SELECT DISTINCT COUNT(*) ": "SELECT COUNT(*) ");
        ensureJoinsAreResolved();
        dbEntityAnalysis.getTableAndJoins().generateFromAndJoins(sb, joinAliasInUse);
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
        TableAndJoins tableAndJoins = dbEntityAnalysis.getTableAndJoins();
        while (joinAlias != null) {
            JoinDescriptor descriptor = tableAndJoins.getJoinDescriptor(joinAlias);
            if (descriptor == null) {
                break;
            }
            String alias = descriptor.getJoinAliasId();
            if (!joinAliasInUse.add(alias)) {
                break;
            }
            joinAlias = descriptor.getDependentAlias();
        }
    }


    /**
     * Called during initialization to load and register all fields.
     *
     * Subclasses can overwrite this to alter which fields and joins are available.
     * The default implementation uses {@link DBEntityAnalysis.DBEntityAnalysisBuilder#loadFieldsAndJoinsFromModel()}
     * which should be sufficient for most use-cases.
     *
     * Returns a builder to enable composing (i.e. enable the subclass to do
     * {@code return super.prepareDBEntityAnalysis().my().Change().Here();}
     */
    protected DBEntityAnalysis.DBEntityAnalysisBuilder<T> prepareDBEntityAnalysis() {
        return DBEntityAnalysis.builder(this.modelClass).loadFieldsAndJoinsFromModel();
    }

    public void getTableFields(StringBuilder sb) {
        boolean first = true;
        for (QueryField queryField : dbEntityAnalysis.getAllSelectableFields()) {
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
        ensureJoinsAreResolved();
        dbEntityAnalysis.getTableAndJoins().generateFromAndJoins(sb, joinAliasInUse);
        getFilter().getFilterQueryString(sb);
        getSort().getSortQueryString(sb);
        getPagination().getOffsetQueryString(sb);
        getPagination().getLimitQueryString(sb);
        return sb.toString();
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
}
