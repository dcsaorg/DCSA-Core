package org.dcsa.util;

import org.dcsa.exception.GetException;

import javax.el.MethodNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A class to help managing filter parameters and filtering of sql result.
 * It parses filter parameters and make sure they are correctly specified.
 * It encodes the filter parameters to be used for pagination links.
 * It creates the SQL (WHERE clause) used for database requests
 * @param <T> the type of the class modeled by this {@code Class}
 */
public class Filter<T> {
    protected static final String FILTER_SPLIT = "=";

    final ExtendedRequest<T> extendedRequest;
    final ExtendedParameters extendedParameters;

    private final List<FilterItem> filters = new ArrayList<>();

    protected Filter(ExtendedRequest<T> extendedRequest, ExtendedParameters extendedParameters) {
        this.extendedRequest = extendedRequest;
        this.extendedParameters = extendedParameters;
    }

    public void addStringFilter(String fieldName, String value) {
        addFilter(fieldName, null, value, false, false, true);
    }

    public void addExactFilter(String fieldName, String value, boolean stringValue) {
        addFilter(fieldName, null, value, true, false, stringValue);
    }

    public void addOrGroupFilter(String fieldName, String value, boolean exactMatch) {
        addFilter(fieldName, null, value, exactMatch, true, true);
    }

    public void addFilter(String fieldName, Class<?> clazz, String fieldValue, boolean exactMatch, boolean orGroup, boolean stringValue) {
        filters.add(new FilterItem(fieldName, clazz, fieldValue, exactMatch, orGroup, stringValue));
    }

    protected void parseFilterParameter(String parameter, String value, boolean fromCursor) throws NoSuchFieldException {
        if (extendedRequest.isCursor()) {
            throw new GetException("Cannot use Filtering while accessing a paginated result using the " + extendedParameters.getPaginationCursorName() + " parameter!");
        }

        try {
            String fieldName = extendedRequest.transformFromJsonNameToFieldName(parameter);
            Class<?> fieldType = extendedRequest.getFieldType(fieldName);
            // Test if the return type is an Enum
            if (fieldType.getEnumConstants() != null) {
                // Return type IS Enum - split a possible list on EnumSplitter defined in extendedParameters and force exact match in filtering
                String[] enumList = value.split(extendedParameters.getEnumSplit());
                for (String enumItem : enumList) {
                    addOrGroupFilter(fieldName, enumItem, true);
                }
            } else if (String.class.equals(fieldType)) {
                if ("NULL".equals(value)) {
                    addExactFilter(fieldName, value, false);
                } else {
                    addStringFilter(fieldName, value);
                }
            } else if (UUID.class.equals(fieldType)) {
                addExactFilter(fieldName, value, !"NULL".equals(value));
            } else if (Integer.class.equals(fieldType) || Long.class.equals(fieldType)) {
                addExactFilter(fieldName, value, true);
            } else if (Boolean.class.equals(fieldType)) {
                if ("TRUE".equals(value.toUpperCase()) || "FALSE".equals(value.toUpperCase())) {
                    addExactFilter(fieldName, value, false);
                } else {
                    throw new GetException("Boolean filter value must be either: (TRUE|FALSE) - value not recognized: " + value + " on filter: " + fieldType.getSimpleName());
                }
            } else {
                throw new GetException("Type on filter (" + parameter + ") not recognized: " + fieldType.getSimpleName());
            }
        } catch (MethodNotFoundException methodNotFoundException) {
            throw new GetException("Getter method corresponding to parameter: " + parameter + " not found!");
        }
        if (!fromCursor) {
            extendedRequest.setNoCursor();
        }
    }

    protected void getFilterQueryString(StringBuilder sb) {
        boolean first = true;
        boolean flag = false;
        for (FilterItem filter : filters) {
            try {
                String columnName = extendedRequest.transformFromFieldNameToColumnName(filter.clazz, filter.fieldName);
                String value = filter.fieldValue;
                if (!first) {
                    flag = manageSortGroup(sb, filter.orGroup, flag);
                } else {
                    first = false;
                    sb.append(" WHERE ");
                    if (filter.orGroup) {
                        sb.append("(");
                        flag = true;
                    }
                }
                insertSortValue(sb, columnName, value, filter);
            } catch (NoSuchFieldException noSuchFieldException) {
                throw new GetException("Cannot map fieldName: " + filter.fieldName + " to a database column name when creating internal sql filter");
            }
        }
        if (flag) {
            sb.append(")");
        }
    }

    boolean manageSortGroup(StringBuilder sb, boolean orGroup, boolean flag) {
        if (orGroup) {
            if (flag) {
                sb.append(" OR ");
            } else {
                sb.append(" AND (");
                return true;
            }
        } else {
            if (flag) {
                sb.append(") AND ");
                return false;
            } else {
                sb.append(" AND ");
            }
        }
        return flag;
    }

    void insertSortValue(StringBuilder sb, String columnName, String value, FilterItem filter) {
        value = sanitizeValue(value);
        if (filter.exactMatch) {
            sb.append(columnName).append("=");
            if (filter.stringValue) {
                sb.append("'").append(value).append("'");
            } else {
                sb.append(value);
            }
        } else {
            sb.append(columnName).append(" like '%").append(value).append("%'");
        }
    }

    // Transform <, >, </, (“, “), (‘, ‘), “, etc. to html code
    // # $ ? /” \”
    protected String sanitizeValue(String value) {
        if (value != null) {
            // More SQL injection prevention could be done
            value = value.replaceAll("-", "&#45;");
            value = value.replaceAll("<", "&#lt;");
            value = value.replaceAll(">", "&#gt;");
            value = value.replaceAll("\\)", "&#40;");
            value = value.replaceAll("\\(", "&#41;");
            value = value.replaceAll("'", "&#apos;");
            value = value.replaceAll("#", "&#35;");
            value = value.replaceAll("\\$", "&#44;");
            value = value.replaceAll("\\?", "&#63;");
            value = value.replaceAll("\\\\", "&#92;");
            value = value.replaceAll("/", "&#47;");
            value = value.replaceAll("%", "&#37;");
        }
        return value;
    }

    protected void encodeFilter(StringBuilder sb) {
        boolean insideOrGroup = false;
        for (FilterItem filter : filters) {
            if (!filter.orGroup || !insideOrGroup) {
                if (sb.length() != 0) {
                    sb.append(ExtendedRequest.PARAMETER_SPLIT);
                }
                try {
                    String jsonName = extendedRequest.transformFromFieldNameToJsonName(filter.clazz, filter.fieldName);
                    sb.append(jsonName);
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Cannot map fieldName: " + filter.fieldName + " to JSON property when creating internal filter-query parameter");
                }
                String fieldValue = filter.fieldValue;
                sb.append(FILTER_SPLIT).append(fieldValue);
                insideOrGroup = filter.orGroup;
            }
        }
    }
}
