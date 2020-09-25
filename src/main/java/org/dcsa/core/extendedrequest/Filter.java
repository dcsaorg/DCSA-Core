package org.dcsa.core.extendedrequest;

import org.dcsa.core.exception.GetException;
import org.dcsa.core.util.ReflectUtility;

import javax.el.MethodNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
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

    public void addFilterItem(FilterItem filterItem) {
        filters.add(filterItem);
    }

    public List<FilterItem> getFilters() {
        return filters;
    }

    protected void parseFilterParameter(String parameter, String value, boolean fromCursor) throws NoSuchFieldException {
        if (extendedRequest.isCursor()) {
            throw new GetException("Cannot use Filtering while accessing a paginated result using the " + extendedParameters.getPaginationCursorName() + " parameter!");
        }

        try {
            Class<?> modelClassToUse = null;
            int pos = parameter.indexOf('.');
            if (pos != -1) {
                String className = parameter.substring(0, pos);
                parameter = parameter.substring(pos + 1);
                try {
                    modelClassToUse = ReflectUtility.getFieldModelClass(extendedRequest.getModelClass(), parameter);
                    if (modelClassToUse == null) {
                        throw new GetException("Specified modelClass not found:" + className + " bound to fieldName:" + parameter);
                    }
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Specified field not found:" + parameter);
                }
            }
            String fieldName = extendedRequest.transformFromJsonNameToFieldName(modelClassToUse, parameter);
            Class<?> fieldType = extendedRequest.getFieldType(modelClassToUse, fieldName);
            // Test if the return type is an Enum
            if (fieldType.getEnumConstants() != null) {
                // Return type IS Enum - split a possible list on EnumSplitter defined in extendedParameters and force exact match in filtering
                String[] enumList = value.split(extendedParameters.getEnumSplit());
                for (String enumItem : enumList) {
                    addFilterItem(FilterItem.addOrGroupFilter(fieldName, modelClassToUse, enumItem, true));
                }
            } else if (String.class.equals(fieldType)) {
                if ("NULL".equals(value)) {
                    addFilterItem(FilterItem.addExactFilter(fieldName, modelClassToUse, value, false));
                } else {
                    addFilterItem(FilterItem.addStringFilter(fieldName, modelClassToUse, value));
                }
            } else if (UUID.class.equals(fieldType)) {
                addFilterItem(FilterItem.addExactFilter(fieldName, modelClassToUse, value, !"NULL".equals(value)));
            } else if (LocalDate.class.equals(fieldType)) {
                addFilterItem(FilterItem.addExactFilter(fieldName, modelClassToUse, value, true));
            } else if (LocalDateTime.class.equals(fieldType) || OffsetDateTime.class.equals(fieldType)) {
                addFilterItem(FilterItem.addExactFilter(fieldName, modelClassToUse, value, true));
            } else if (Integer.class.equals(fieldType) || Long.class.equals(fieldType)) {
                addFilterItem(FilterItem.addExactFilter(fieldName, modelClassToUse, value, true));
            } else if (Boolean.class.equals(fieldType)) {
                if ("TRUE".equals(value.toUpperCase()) || "FALSE".equals(value.toUpperCase())) {
                    addFilterItem(FilterItem.addExactFilter(fieldName, modelClassToUse, value, false));
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
                String columnName = extendedRequest.transformFromFieldNameToColumnName(filter.getClazz(), filter.getFieldName());
                String value = filter.getFieldValue();
                if (!first) {
                    flag = manageFilterGroup(sb, filter.isOrGroup(), flag);
                } else {
                    first = false;
                    sb.append(" WHERE ");
                    if (filter.isOrGroup()) {
                        sb.append("(");
                        flag = true;
                    }
                }
                insertFilterValue(sb, columnName, value, filter);
            } catch (NoSuchFieldException noSuchFieldException) {
                throw new GetException("Cannot map fieldName: " + filter.getFieldName() + " to a database column name when creating internal sql filter");
            }
        }
        if (flag) {
            sb.append(")");
        }
    }

    public boolean manageFilterGroup(StringBuilder sb, boolean orGroup, boolean flag) {
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

    public void insertFilterValue(StringBuilder sb, String columnName, String value, FilterItem filter) {
        value = sanitizeValue(value);
        if (filter.getClazz() != null && filter.getClazz() != extendedRequest.getModelClass()) {
            extendedRequest.getTableName(filter.getClazz(), sb);
            sb.append(".");
        }
        sb.append(columnName);
        if (filter.isExactMatch()) {
            sb.append("=");
            if (filter.isStringValue()) {
                sb.append("'").append(value).append("'");
            } else {
                sb.append(value);
            }
        } else {
            sb.append(" like '%").append(value).append("%'");
        }
    }

    // Transform <, >, </, (“, “), (‘, ‘), “, etc. to html code
    // # $ ? /” \”
    protected String sanitizeValue(String value) {
        if (value != null) {
            if (value.matches("[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}")) {
                // value matches a UUID - no need to be sanitized
                return value;
            } else if (value.matches("\\d{4}-\\d\\d-\\d\\d(T\\d\\d:\\d\\d:\\d\\d[\\+\\- ]\\d\\d:\\d\\d)?")) {
                // value matches a date - no need to be sanitized
                return value;
            }
            // More SQL injection prevention could be done
            value = value.replaceAll("#", "&#35;");
            value = value.replaceAll("-", "&#45;");
            value = value.replaceAll("<", "&#lt;");
            value = value.replaceAll(">", "&#gt;");
            value = value.replaceAll("\\)", "&#40;");
            value = value.replaceAll("\\(", "&#41;");
            value = value.replaceAll("'", "&#apos;");
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
            if (!filter.isOrGroup() || !insideOrGroup) {
                if (sb.length() != 0) {
                    sb.append(ExtendedRequest.PARAMETER_SPLIT);
                }
                try {
                    String jsonName = extendedRequest.transformFromFieldNameToJsonName(filter.getClazz(), filter.getFieldName());
                    sb.append(jsonName);
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Cannot map fieldName: " + filter.getFieldName() + " to JSON property when creating internal filter-query parameter");
                }
                String fieldValue = filter.getFieldValue();
                sb.append(FILTER_SPLIT).append(fieldValue);
                insideOrGroup = filter.isOrGroup();
            }
        }
    }
}
