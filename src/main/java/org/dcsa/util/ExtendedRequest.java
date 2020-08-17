package org.dcsa.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.dcsa.exception.GetException;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.query.Criteria;
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
import java.util.*;

public class ExtendedRequest<T> {

    private static final String PARAMETER_SPLIT = "&";
    private static final String FILTER_SPLIT = "=";
    private static final String LIMIT_SPLIT = "=";
    private static final String CURSOR_SPLIT = "=";
    private static final String INDEX_SPLIT = "=";
    private static final String SORT_SPLIT = "=";
    private static final String SORT_SEPARATOR = ",";

    private Sort sort;
    private Filter filter;
    private Integer limit;
    private Integer indexCursor;

    private final ExtendedParameters extendedParameters;
    private final Class<T> modelClass;
    private Boolean isCursor = null;


    public ExtendedRequest(ExtendedParameters extendedParameters, Class<T> modelClass) {
        this.modelClass = modelClass;
        this.extendedParameters = extendedParameters;
        limit = extendedParameters.getDefaultPageSize();
    }

    public void parseParameter(Map<String, String> params) {
        parseParameter(params, false);
    }

    public void parseParameter(Map<String, String> params, boolean fromCursor) {
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            if (!extendedParameters.getReservedParameters().contains(key)) {
                parseParameter(key, entry.getValue(), fromCursor);
            }
        }
    }

    private void parseParameter(String key, String value, boolean fromCursor) {
        if (extendedParameters.getSortParameterName().equals(key)) {
            // Parse sorting
            parseSortParameter(value, fromCursor);
        } else if (extendedParameters.getPaginationPageSizeName().equals(key)) {
            // Parse limit
            parseLimitParameter(value, fromCursor);
        } else if (extendedParameters.getPaginationCursorName().equals(key)) {
            // Parse cursor
            parseCursorParameter(value);
        } else if (extendedParameters.getIndexCursor().equals(key) && fromCursor) {
            // Parse internal pagination cursor
            parseInternalPagniationCursor(value);
        } else {
            // Parse filtering
            parseFilterParameter(key, value, fromCursor);
        }
    }

    private void parseInternalPagniationCursor(String cursorValue) {
        indexCursor = Integer.parseInt(cursorValue);
    }

    private void parseSortParameter(String sortParameter, boolean fromCursor) {
        if (isCursor != null && isCursor) {
            throw new GetException("Cannot use Sorting while accessing a paginated result using the " + extendedParameters.getPaginationCursorName() + " parameter!");
        }

        if (sortParameter != null) {
            String[] sortableValues = sortParameter.split(SORT_SEPARATOR);
            for (String sortableValue: sortableValues) {
                String[] fieldAndDirection = sortableValue.split(extendedParameters.getSortDirectionSeparator());
                try {
                    // Verify that the field exists on the model class and transform it from JSON-name to FieldName
                    fieldAndDirection[0] = ReflectUtility.transformFromJsonNameToFieldName(modelClass, fieldAndDirection[0]);
                    updateSort(fieldAndDirection);
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Sort parameter not correctly specified. Field: " + fieldAndDirection[0] + " does not exist. Use - {fieldName}" + extendedParameters.getSortDirectionSeparator() + "[ASC|DESC]");
                }
            }
            if (!fromCursor) {
                isCursor = false;
            }
        }
    }

    private void updateSort(String[] fieldDirection) {
        if (fieldDirection.length == 1) {
            // Direction is not specified - use ASC as default
            updateSort(Sort.Direction.ASC, fieldDirection[0]);
        } else if (fieldDirection.length == 2) {
            Sort.Direction direction = parseDirection(fieldDirection[1]);
            updateSort(direction, fieldDirection[0]);
        } else {
            throw new GetException("Sort parameter not correctly specified. Use - {fieldName} " + extendedParameters.getSortDirectionSeparator() + "[ASC|DESC]");
        }
    }

    private void updateSort(Sort.Direction direction, String column) {
        if (sort == null) {
            sort = Sort.by(direction, column);
        } else {
            Sort newSort = Sort.by(direction, column);
            sort = sort.and(newSort);
        }
    }

    private Sort.Direction parseDirection(String direction) {
        if (extendedParameters.getSortDirectionAscendingName().equals(direction)) {
            return Sort.Direction.ASC;
        } else if (extendedParameters.getSortDirectionDescendingName().equals(direction)) {
            return Sort.Direction.DESC;
        } else {
            throw new GetException("Sort parameter not correctly specified. Sort direction: " + direction + " is unknown. Use either " + extendedParameters.getSortDirectionAscendingName() + " or " + extendedParameters.getSortDirectionDescendingName());
        }
    }

    private void parseLimitParameter(String value, boolean fromCursor) {
        if (isCursor != null && isCursor) {
            throw new GetException("Cannot use the Limit parameter while accessing a paginated result using the " + extendedParameters.getPaginationCursorName() + " parameter!");
        }

        try {
            limit = Integer.parseInt(value);
            if (!fromCursor) {
                isCursor = false;
            }
        } catch (NumberFormatException numberFormatException) {
            throw new GetException("Unknown " + extendedParameters.getPaginationPageSizeName() + " value:" + value + ". Must be a Number!");
        }
    }

    private void parseFilterParameter(String parameter, String value, boolean fromCursor) {
        if (isCursor != null && isCursor) {
            throw new GetException("Cannot use Filtering while accessing a paginated result using the " + extendedParameters.getPaginationCursorName() + " parameter!");
        }

        try {
            String field = ReflectUtility.transformFromJsonNameToFieldName(modelClass, parameter);
            if (filter == null) {
                filter = new Filter();
            }
            filter.addFilter(field, value);
            if (!fromCursor) {
                isCursor = false;
            }
        } catch (NoSuchFieldException noSuchFieldException) {
            throw new GetException("Filter parameter not recognized: " + parameter);
        }
    }

    private void parseCursorParameter(String cursorValue) {
        if (isCursor != null && !isCursor) {
            throw new GetException("Cannot use " + extendedParameters.getPaginationCursorName() + " parameter in combination with Sorting, Filtering or Limiting of the result!");
        }

        byte[] decodedCursor = Base64.getUrlDecoder().decode(cursorValue);

        // If encryption is used - decrypt the parameter
        String encryptionKey = extendedParameters.getEncryptionKey();
        if (encryptionKey != null) {
            decodedCursor = decrypt(encryptionKey, decodedCursor);
        }

        Map<String, String> params = convertToQueryStringToHashMap(new String(decodedCursor, StandardCharsets.UTF_8));
        parseParameter(params, true);
        isCursor = true;
    }

    public String getQuery() {
        return "select * from \"" + getTableName() + "\"" + getFilterString() + getSortString() + getOffsetString() + getLimitString();
    }

    public String getTableName() {
        Table table = modelClass.getAnnotation(Table.class);
        if (table == null) {
            throw new GetException("@Table not defined on class:" + modelClass.getSimpleName());
        }
        return table.value();
    }

    public boolean ignoreUnknownProperties() {
        JsonIgnoreProperties jsonIgnoreProperties = modelClass.getAnnotation(JsonIgnoreProperties.class);
        return jsonIgnoreProperties != null && jsonIgnoreProperties.ignoreUnknown();
    }

    public T getModelClassInstance(Row row, RowMetadata meta) {
        try {
            JsonSubTypes jsonSubTypes = modelClass.getAnnotation(JsonSubTypes.class);
            JsonTypeInfo jsonTypeInfo = modelClass.getAnnotation(JsonTypeInfo.class);
            if (jsonSubTypes != null && jsonTypeInfo != null) {
                String property = jsonTypeInfo.property();
                String columnName = ReflectUtility.transformFromFieldNameToColumnName(modelClass, property);
                Object value = row.get(columnName);
                for (JsonSubTypes.Type type : jsonSubTypes.value()) {
                    if (type.name().equals(value)) {
                        Constructor<?> constructor = type.value().getDeclaredConstructor();
                        return (T) constructor.newInstance();
                    }
                }
                throw new GetException("Unmatched sub-type: " + value + " of " + modelClass.getSimpleName());
            } else {
                Constructor<T> constructor = modelClass.getDeclaredConstructor();
                return constructor.newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            throw new GetException("Error when creating a new instance of: " + modelClass.getSimpleName());
        }
    }

    private String getFilterString() {
        if (filter != null) {
            List<Criteria> criteriaList = new ArrayList<>();
            for (int i = 0; i < filter.getFilterSize(); i++) {
                try {
                    String field = ReflectUtility.transformFromFieldNameToColumnName(modelClass, filter.getFieldName(i));
                    String value = filter.getFieldValue(i);
                    // TODO: SQL injection should be tested here...
                    criteriaList.add(Criteria.where(field).like("%" + value + "%"));
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Cannot map fieldName: " + filter.getFieldName(i) + " to a database column name when creating internal sql filter");
                }
            }
            return " WHERE " + Criteria.from(criteriaList).toString();
        } else {
            return "";
        }
    }

    private String getSortString() {
        if (sort != null) {
            StringBuilder sb = new StringBuilder(" ORDER BY ");
            for (Sort.Order order : sort.toList()) {
                if (sb.length() != " ORDER BY ".length()) {
                    sb.append(", ");
                }
                // Verify that the field exists
                String fieldName = order.getProperty();
                try {
                    String columnName = ReflectUtility.transformFromFieldNameToColumnName(modelClass, fieldName);
                    sb.append(columnName);
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Cannot map fieldName: " + fieldName + " to a database column name when creating internal sql sorting");
                }
                if (order.isAscending()) {
                    sb.append(" ASC");
                } else {
                    sb.append(" DESC");
                }
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    private String getOffsetString() {
        if (indexCursor != null) {
            return " OFFSET " + indexCursor;
        } else {
            return "";
        }
    }

    private String getLimitString() {
        if (limit != null) {
            return " LIMIT " + limit;
        } else {
            return "";
        }
    }

    public static Map<String, String> convertToQueryStringToHashMap(String source) {
        Map<String, String> data = new HashMap<>();
        final String[] arrParameters = source.split(PARAMETER_SPLIT);
        for (final String tempParameterString : arrParameters) {
            final String[] arrTempParameter = tempParameterString.split("=");
            if (arrTempParameter.length >= 2) {
                final String parameterKey = arrTempParameter[0];
                final String parameterValue = arrTempParameter[1];
                data.put(parameterKey, parameterValue);
            } else {
                final String parameterKey = arrTempParameter[0];
                data.put(parameterKey, "");
            }
        }
        return data;
    }

    private byte[] encrypt(String key, byte[] text) {
        try {
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            return cipher.doFinal(text);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException exception) {
            throw new GetException("Error creating encryption algorithm:" + exception.getMessage());
        }
    }

    private byte[] decrypt(String key, byte[] text) {
        try {
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");

            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            return cipher.doFinal(text);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException exception) {
            throw new GetException("Error creating encryption algorithm:" + exception.getMessage());
        }
    }

    public void insertPaginationHeaders(ServerHttpResponse response, ServerHttpRequest request) {
        if (limit != null) {
            response.getHeaders().add(extendedParameters.getPaginationCurrentPageName(), getURI(request) + "?" + getHeaderPageCursor(PageRequest.CURRENT));
            response.getHeaders().add(extendedParameters.getPaginationNextPageName(), getURI(request) + "?" + getHeaderPageCursor(PageRequest.NEXT));
            response.getHeaders().add(extendedParameters.getPaginationPreviousPageName(), getURI(request) + "?" + getHeaderPageCursor(PageRequest.PREVIOUS));
            response.getHeaders().add(extendedParameters.getPaginationFirstPageName(), getURI(request) + "?" + getHeaderPageCursor(PageRequest.FIRST));
        } else {
            StringBuilder sb = new StringBuilder();
            encodeSort(sb);
            encodeFilter(sb);
            encodeLimit(sb);
            response.getHeaders().add(extendedParameters.getPaginationCurrentPageName(), getURI(request) + "?" + sb.toString());
        }
    }

    private String getURI(ServerHttpRequest request) {
        return request.getURI().getScheme() + "://" + request.getURI().getRawAuthority() + request.getURI().getRawPath();
    }

    private String getHeaderPageCursor(PageRequest page) {
        StringBuilder sb = new StringBuilder();
        encodeSort(sb);
        encodeFilter(sb);
        encodeLimit(sb);
        encodePagination(sb, page);
        byte[] parameters = sb.toString().getBytes(StandardCharsets.UTF_8);

        // If encryption is used - encrypt the parameter string
        String encryptionKey = extendedParameters.getEncryptionKey();
        if (encryptionKey != null) {
            parameters = encrypt(encryptionKey, parameters);
        }
        return extendedParameters.getPaginationCursorName() + CURSOR_SPLIT + Base64.getUrlEncoder().encodeToString(parameters);
    }

    private void encodeSort(StringBuilder sb) {
        if (sort != null) {
            if (sb.length() != 0) {
                sb.append(PARAMETER_SPLIT);
            }
            sb.append(extendedParameters.getSortParameterName()).append(SORT_SPLIT);
            int size = sb.length();
            for (Sort.Order order : sort.toList()) {
                if (size != sb.length()) {
                    sb.append(SORT_SEPARATOR);
                }
                try {
                    String jsonName = ReflectUtility.transformFromFieldNameToJsonName(modelClass, order.getProperty());
                    sb.append(jsonName);
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Cannot map fieldName: " + order.getProperty() + " to JSON property when creating internal sort-query parameter");
                }
                // Don't specify ASC direction - this is default
                if (order.isDescending()) {
                    sb.append(extendedParameters.getSortDirectionSeparator());
                    sb.append(extendedParameters.getSortDirectionDescendingName());
                }
            }
        }
    }

    private void encodeFilter(StringBuilder sb) {
        if (filter != null) {
            for (int i = 0; i < filter.getFilterSize(); i++) {
                if (sb.length() != 0) {
                    sb.append(PARAMETER_SPLIT);
                }
                try {
                    String jsonName = ReflectUtility.transformFromFieldNameToJsonName(modelClass, filter.getFieldName(i));
                    sb.append(jsonName);
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Cannot map fieldName: " + filter.getFieldName(i) + " to JSON property when creating internal filter-query parameter");
                }
                String fieldValue = filter.getFieldValue(i);
                sb.append(FILTER_SPLIT).append(fieldValue);
            }
        }
    }

    private void encodeLimit(StringBuilder sb) {
        if (limit != null) {
            if (sb.length() != 0) {
                sb.append(PARAMETER_SPLIT);
            }
            sb.append(extendedParameters.getPaginationPageSizeName()).append(LIMIT_SPLIT).append(limit);
        }
    }

    private void encodePagination(StringBuilder sb, PageRequest page) {
        if (indexCursor != null) {
            if (page == PageRequest.CURRENT) {
                encodePage(sb, indexCursor);
            } else if (page == PageRequest.NEXT) {
                encodePage(sb, indexCursor + limit);
            } else if (indexCursor > limit) {
                encodePage(sb, indexCursor - limit);
            }
        } else {
            // Current page is FIRST page...
            if (page == PageRequest.NEXT) {
                encodePage(sb, limit);
            }
        }
    }

    private void encodePage(StringBuilder sb, Integer index) {
        if (sb.length() != 0) {
            sb.append(PARAMETER_SPLIT);
        }
        sb.append(extendedParameters.getIndexCursor()).append(INDEX_SPLIT).append(index);
    }
}
