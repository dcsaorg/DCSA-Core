package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.dcsa.core.exception.GetException;
import org.dcsa.core.query.DBEntityAnalysis;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.OrderByField;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    private final List<SortDesc> sortDescriptionList = new ArrayList<>();

    public Sort(ExtendedRequest<T> extendedRequest, ExtendedParameters extendedParameters) {
        this.extendedRequest = extendedRequest;
        this.extendedParameters = extendedParameters;
    }

    protected void parseSortParameter(String sortParameter, boolean fromCursor) {
        if (extendedRequest.isCursor()) {
            throw new GetException("Cannot use Sorting while accessing a paginated result using the " + extendedParameters.getPaginationCursorName() + " parameter!");
        }

        if (sortParameter != null) {
            String[] sortableValues = sortParameter.split(SORT_SEPARATOR);
            DBEntityAnalysis<T> dbEntityAnalysis = extendedRequest.getDbEntityAnalysis();
            for (String sortableValue : sortableValues) {
                String[] fieldAndDirection = sortableValue.split(extendedParameters.getSortDirectionSeparator());
                String jsonName = fieldAndDirection[0];
                // Verify that the field exists on the model class and transform it from JSON-name to FieldName
                QueryField queryField;
                try {
                    queryField = dbEntityAnalysis.getQueryFieldFromJSONName(jsonName);
                } catch (IllegalArgumentException e) {
                    throw new GetException("Sort parameter not correctly specified. Field: " + fieldAndDirection[0]
                            + " does not exist. Use - {fieldName}" + extendedParameters.getSortDirectionSeparator()
                            + "[ASC|DESC]");
                }
                extendedRequest.markQueryFieldInUse(queryField);

                if (queryField.getCombinedModelField().isAnnotationPresent(JsonIgnore.class)) {
                    throw new GetException("Cannot sort on an Ignored field: " + jsonName);
                }
                switch (fieldAndDirection.length) {
                    case 1:
                        // Direction is not specified - use ASC as default
                        updateSort(org.springframework.data.domain.Sort.Direction.ASC, queryField);
                        break;
                    case 2:
                        updateSort(parseDirection(fieldAndDirection[1]), queryField);
                        break;
                    default:
                        throw new GetException("Sort parameter not correctly specified. Use - {fieldName} " + extendedParameters.getSortDirectionSeparator() + "[ASC|DESC]");
                }
            }
            if (!fromCursor) {
                extendedRequest.setNoCursor();
            }
        }
    }

    private void updateSort(org.springframework.data.domain.Sort.Direction direction, QueryField queryField) {
        sortDescriptionList.add(SortDesc.of(queryField, direction));
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

    protected List<OrderByField> getOrderByFields() {
        return sortDescriptionList.stream().map(sortDesc -> {
            // Use select name where possible due to
            // https://github.com/spring-projects/spring-data-jdbc/issues/968
            Column column = sortDesc.queryField.getSelectColumn();
            if (column == null) {
                column = sortDesc.queryField.getInternalQueryColumn();
            }
            return OrderByField.from(column, sortDesc.sortDirection);
        }).collect(Collectors.toList());
    }

    protected void encodeSort(StringBuilder sb) {
        if (!sortDescriptionList.isEmpty()) {
            if (sb.length() != 0) {
                sb.append(ExtendedRequest.PARAMETER_SPLIT);
            }
            sb.append(extendedParameters.getSortParameterName()).append(SORT_SPLIT);
            boolean first = true;
            for (SortDesc sortDesc : sortDescriptionList) {
                if (!first) {
                    sb.append(SORT_SEPARATOR);
                }
                sb.append(sortDesc.getQueryField().getJsonName());
                // Don't specify ASC direction - this is default
                if (sortDesc.getSortDirection().isDescending()) {
                    sb.append(extendedParameters.getSortDirectionSeparator());
                    sb.append(extendedParameters.getSortDirectionDescendingName());
                }
                first = false;
            }
        }
    }

    @Data(staticConstructor = "of")
    private static class SortDesc {
        final QueryField queryField;
        final org.springframework.data.domain.Sort.Direction sortDirection;
    }

}
