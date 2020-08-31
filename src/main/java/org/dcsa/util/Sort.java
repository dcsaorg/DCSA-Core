package org.dcsa.util;

import org.dcsa.exception.GetException;

/**
 * A class to help managing sorting parameters and ordering of the sql result.
 * It parses sorting parameters and make sure they are correctly specified.
 * It encodes the sorting parameters to be used for pagination links.
 * It creates the SQL (ORDER BY clause) used for database requests
 * @param <T> the type of the class modeled by this {@code Class}
 */
public class Sort<T> {
    private static final String SORT_SPLIT = "=";
    private static final String SORT_SEPARATOR = ",";

    private final ExtendedRequest<T> extendedRequest;
    private final ExtendedParameters extendedParameters;
    private org.springframework.data.domain.Sort sorting;

    protected Sort(ExtendedRequest<T> extendedRequest, ExtendedParameters extendedParameters) {
        this.extendedRequest = extendedRequest;
        this.extendedParameters = extendedParameters;
    }

    protected void parseSortParameter(String sortParameter, boolean fromCursor) {
        if (extendedRequest.isCursor()) {
            throw new GetException("Cannot use Sorting while accessing a paginated result using the " + extendedParameters.getPaginationCursorName() + " parameter!");
        }

        if (sortParameter != null) {
            String[] sortableValues = sortParameter.split(SORT_SEPARATOR);
            for (String sortableValue: sortableValues) {
                String[] fieldAndDirection = sortableValue.split(extendedParameters.getSortDirectionSeparator());
                try {
                    // Verify that the field exists on the model class and transform it from JSON-name to FieldName
                    String fieldName = extendedRequest.transformFromJsonNameToFieldName(fieldAndDirection[0]);
                    switch (fieldAndDirection.length) {
                        case 1:
                            // Direction is not specified - use ASC as default
                            updateSort(org.springframework.data.domain.Sort.Direction.ASC, fieldName); break;
                        case 2:
                            updateSort(parseDirection(fieldAndDirection[1]), fieldName); break;
                        default:
                            throw new GetException("Sort parameter not correctly specified. Use - {fieldName} " + extendedParameters.getSortDirectionSeparator() + "[ASC|DESC]");
                    }
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Sort parameter not correctly specified. Field: " + fieldAndDirection[0] + " does not exist. Use - {fieldName}" + extendedParameters.getSortDirectionSeparator() + "[ASC|DESC]");
                }
            }
            if (!fromCursor) {
                extendedRequest.setNoCursor();
            }
        }
    }

    private void updateSort(org.springframework.data.domain.Sort.Direction direction, String fieldName) {
        if (sorting == null) {
            sorting = org.springframework.data.domain.Sort.by(direction, fieldName);
        } else {
            org.springframework.data.domain.Sort newSort = org.springframework.data.domain.Sort.by(direction, fieldName);
            sorting = sorting.and(newSort);
        }
    }

    private org.springframework.data.domain.Sort.Direction parseDirection(String direction) {
        if (extendedParameters.getSortDirectionAscendingName().equals(direction)) {
            return org.springframework.data.domain.Sort.Direction.ASC;
        } else if (extendedParameters.getSortDirectionDescendingName().equals(direction)) {
            return org.springframework.data.domain.Sort.Direction.DESC;
        } else {
            throw new GetException("Sort parameter not correctly specified. Sort direction: " + direction + " is unknown. Use either " + extendedParameters.getSortDirectionAscendingName() + " or " + extendedParameters.getSortDirectionDescendingName());
        }
    }

    protected void getSortQueryString(StringBuilder sb) {
        if (sorting != null) {
            sb.append(" ORDER BY ");
            boolean first = true;
            for (org.springframework.data.domain.Sort.Order order : sorting.toList()) {
                if (!first) {
                    sb.append(", ");
                }
                // Verify that the field exists
                String fieldName = order.getProperty();
                try {
                    String columnName = extendedRequest.transformFromFieldNameToColumnName(fieldName);
                    sb.append(columnName);
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Cannot map fieldName: " + fieldName + " to a database column name when creating internal sql sorting");
                }
                if (order.isAscending()) {
                    sb.append(" ASC");
                } else {
                    sb.append(" DESC");
                }
                first = false;
            }
        }
    }

    protected void encodeSort(StringBuilder sb) {
        if (sorting != null) {
            if (sb.length() != 0) {
                sb.append(ExtendedRequest.PARAMETER_SPLIT);
            }
            sb.append(extendedParameters.getSortParameterName()).append(SORT_SPLIT);
            boolean first = true;
            for (org.springframework.data.domain.Sort.Order order : sorting.toList()) {
                if (!first) {
                    sb.append(SORT_SEPARATOR);
                }
                try {
                    String jsonName = extendedRequest.transformFromFieldNameToJsonName(order.getProperty());
                    sb.append(jsonName);
                } catch (NoSuchFieldException noSuchFieldException) {
                    throw new GetException("Cannot map fieldName: " + order.getProperty() + " to JSON property when creating internal sort-query parameter");
                }
                // Don't specify ASC direction - this is default
                if (order.isDescending()) {
                    sb.append(extendedParameters.getSortDirectionSeparator());
                    sb.append(extendedParameters.getSortDirectionDescendingName());
                }
                first = false;
            }
        }
    }
}
