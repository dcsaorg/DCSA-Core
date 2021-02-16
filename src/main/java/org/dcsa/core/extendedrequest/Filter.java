package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dcsa.core.exception.GetException;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.annotation.Transient;

import javax.el.MethodNotFoundException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
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
    int bindCounter = 0;
    int orGroupCounter = 0;

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

    public int getNewBindCounter() {
        return ++bindCounter;
    }

    public int getOrGroupCounter() {
        return ++orGroupCounter;
    }

    protected void parseFilterParameter(String parameter, String value, boolean fromCursor) {
        QueryField queryField;
        Field javaField;
        Class<?> fieldType;
        if (extendedRequest.isCursor()) {
            throw new GetException("Cannot use Filtering while accessing a paginated result using the " + extendedParameters.getPaginationCursorName() + " parameter!");
        }

        try {
            queryField = this.extendedRequest.getQueryFieldFromJsonName(parameter);
        } catch (NoSuchFieldException e) {
            throw new GetException("Getter method corresponding to parameter: " + parameter + " not found!");
        }

        javaField = queryField.getCombinedModelField();

        if (javaField != null) {
            if (javaField.isAnnotationPresent(JsonIgnore.class)) {
                throw new GetException("Cannot filter on an Ignored field: " + parameter);
            }
            if (javaField.isAnnotationPresent(Transient.class)) {
                throw new GetException("Cannot filter on a Transient field: " + parameter);
            }
        }
        fieldType = queryField.getType();

        if (fieldType.isEnum()) {
            // Return type IS Enum - split a possible list on EnumSplitter (since this is an Enum - no special characters are allowed). If
            // enumSplit value is a "," or a "|" this will work fine
            String[] enumList = value.split(extendedParameters.getEnumSplit());
            int orGroup = getOrGroupCounter();
            for (String enumItem : enumList) {
                addFilterItem(FilterItem.addOrGroupFilter(queryField, enumItem, extendedParameters.getForceExactEnumMatch(), getNewBindCounter(), orGroup));
            }
        } else if (String.class.equals(fieldType)) {
            if ("NULL".equals(value)) {
                addFilterItem(FilterItem.addExactFilter(queryField, value, false, getNewBindCounter()));
            } else {
                addFilterItem(FilterItem.addStringFilter(queryField, value, true, getNewBindCounter()));
            }
        } else if (UUID.class.equals(fieldType)) {
            addFilterItem(FilterItem.addExactFilter(queryField, !"NULL".equals(value) ? UUID.fromString(value) : value, !"NULL".equals(value), getNewBindCounter()));
        } else if (LocalDate.class.equals(fieldType)) {
            String dateFormat = queryField.getDatePattern();
            if (dateFormat == null) {
                dateFormat = extendedParameters.getSearchableDateFormat();
            }
            addFilterItem(FilterItem.addDateFilter(queryField, dateFormat, value, true, getNewBindCounter()));
        } else if (LocalDateTime.class.equals(fieldType) || OffsetDateTime.class.equals(fieldType)) {
            String dateFormat = queryField.getDatePattern();
            if (dateFormat == null) {
                dateFormat = extendedParameters.getSearchableDateTimeFormat();
            }
            addFilterItem(FilterItem.addDateFilter(queryField, dateFormat, value, true, getNewBindCounter()));
        } else if (Integer.class.equals(fieldType) || Long.class.equals(fieldType) || BigDecimal.class.equals(fieldType)) {
            addFilterItem(FilterItem.addNumberFilter(queryField, value, getNewBindCounter()));
        } else if (Boolean.class.equals(fieldType)) {
            if ("TRUE".equalsIgnoreCase(value)) {
                addFilterItem(FilterItem.addExactFilter(queryField, Boolean.TRUE, false, getNewBindCounter()));
            } else if ("FALSE".equalsIgnoreCase(value)) {
                addFilterItem(FilterItem.addExactFilter(queryField, Boolean.FALSE, false, getNewBindCounter()));
            } else {
                throw new GetException("Boolean filter value must be either: (TRUE|FALSE) - value not recognized: " + value + " on filter: " + fieldType.getSimpleName());
            }
        } else {
            throw new GetException("Type on filter (" + parameter + ") not recognized: " + fieldType.getSimpleName());
        }

        if (!fromCursor) {
            extendedRequest.setNoCursor();
        }
    }

    protected void getFilterQueryString(StringBuilder sb) {
        boolean first = true;
        int orGroupNumber = 0;
        for (FilterItem filter : getFilters()) {
            if (!first) {
                orGroupNumber = manageFilterGroup(sb, filter.getOrGroup(), orGroupNumber);
            } else {
                first = false;
                sb.append(" WHERE ");
                if (filter.getOrGroup() > 0) {
                    sb.append("(");
                    orGroupNumber = filter.getOrGroup();
                }
            }
            insertFilterValue(sb, filter);
        }
        if (orGroupNumber != 0) {
            sb.append(")");
        }
    }

    public int manageFilterGroup(StringBuilder sb, int orGroup, int currentOrGroup) {
        if (currentOrGroup > 0) {
            // Scenario where we are inside an or-group
            if (orGroup == currentOrGroup) {
                // We are inside an or-group
                sb.append(" OR ");
            } else if (orGroup > 0){
                // We are inside an or-group and want to start a new or-group
                sb.append(") AND (");
            } else {
                // We are inside an or-group and no new group is starting
                sb.append(") AND ");
            }
        } else {
            // We are NOT inside an or group
            if (orGroup > 0) {
                // Scenario where we are NOT inside an or-group and want to start a new or-group
                sb.append(" AND (");
            } else {
                // Normal scenario without or-group
                sb.append(" AND ");
            }
        }
        return orGroup;
    }

    public void insertFilterValue(StringBuilder sb, FilterItem filter) {
        if (filter.getDateFormat() != null) {
            sb.append("TO_CHAR(");
        } else if (!filter.isStringValue() && !filter.isExactMatch()) {
            sb.append("CAST(");
        }
        sb.append(filter.getQueryField().getQueryInternalName());
        if (filter.getDateFormat() != null) {
            sb.append(" , '").append(filter.getDateFormat()).append("')");
        } else if (!filter.isStringValue() && !filter.isExactMatch()) {
            sb.append(" AS VARCHAR)");
        }
        if (filter.isExactMatch()) {
            sb.append("=");
        } else {
            if (filter.isIgnoreCase()) {
                sb.append(" ILIKE ");
            } else {
                sb.append(" LIKE ");
            }
        }
        sb.append(":").append(filter.getBindName());
    }

    protected void encodeFilter(StringBuilder sb) {
        int insideOrGroup = 0;
        for (FilterItem filter : filters) {
            String jsonName = filter.getQueryField().getJsonName();
            String fieldValue = filter.getFieldValue().toString();
            if (insideOrGroup == 0 || insideOrGroup != filter.getOrGroup()) {
                if (sb.length() != 0) {
                    sb.append(ExtendedRequest.PARAMETER_SPLIT);
                }
                sb.append(jsonName);
                sb.append(FILTER_SPLIT).append(fieldValue);
            } else {
                // Used for multiple enum values on an enumerated fieldValue
                sb.append(extendedParameters.getEnumSplit());
                sb.append(fieldValue);
            }
            insideOrGroup = filter.getOrGroup();
        }
    }
}
