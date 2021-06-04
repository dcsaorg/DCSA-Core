package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.exception.GetException;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.query.impl.ComparisonType;
import org.dcsa.core.query.impl.DelegatingCursorBackedFilterCondition;
import org.dcsa.core.query.impl.InlineableFilterCondition;
import org.springframework.data.annotation.Transient;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.*;
import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.MutableBindings;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.dcsa.core.query.impl.QueryGeneratorUtils.andAllFilters;

@RequiredArgsConstructor
public class QueryParameterParser<T> {

    protected final ExtendedParameters extendedParameters;
    protected final R2dbcDialect r2dbcDialect;
    protected final DBEntityAnalysis<T> dbAnalysis;
    protected final List<FilterCondition> filters = new ArrayList<>();
    private final Map<String, List<String>> parsedParameters = new LinkedHashMap<>();
    protected final Set<QueryField> referencedFields = new HashSet<>();

    private MutableBindings mutableBindings;
    private boolean used = false;

    protected MutableBindings getMutableBindings() {
        if (mutableBindings == null) {
            mutableBindings = new MutableBindings(r2dbcDialect.getBindMarkersFactory().create());
        }
        return mutableBindings;
    }

    public CursorBackedFilterCondition build() {
        if (used) {
            throw new IllegalStateException("build must only be called once");
        }
        used = true;
        return DelegatingCursorBackedFilterCondition.of(
                andAllFilters(filters, false),
                immutableCopy(parsedParameters),
                Collections.unmodifiableSet(referencedFields),
                getMutableBindings()
        );
    }

    private static Map<String, List<String>> immutableCopy(Map<String, List<String>> orig) {
        LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>(orig.size());
        for (Map.Entry<String, List<String>> entry : orig.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    public void parseQueryParameter(Map<String, List<String>> queryParameters) {
        parseQueryParameter(queryParameters, (a) -> false);
    }

    public void parseQueryParameter(Map<String, List<String>> queryParameters, Predicate<String> ignoredParameter) {
        for (Map.Entry<String, List<String>> queryParameter : queryParameters.entrySet()) {
            final String parameterKey = queryParameter.getKey();
            List<String> values = queryParameter.getValue();
            String jsonName = parameterKey;
            String fieldAttribute = null;
            if (ignoredParameter.test(parameterKey)) {
                continue;
            }
            if (parameterKey.endsWith("]") && extendedParameters.getQueryParameterAttributeHandling() == QueryParameterAttributeHandling.PARAMETER_NAME_ARRAY_NOTATION) {
                String[] parts = parameterKey.split("\\[", 2);
                if (parts.length == 2) {
                    jsonName = parts[0];
                    if (ignoredParameter.test(jsonName)) {
                        continue;
                    }
                    fieldAttribute = parts[1].substring(0, parts[1].length() - 1);
                }
            } else if (extendedParameters.getQueryParameterAttributeHandling() == QueryParameterAttributeHandling.PARAMETER_NAME_SUFFIX) {
                String separator = extendedParameters.getQueryParameterAttributeSeparator();
                int index = parameterKey.indexOf(separator);
                if (index > -1) {
                    jsonName = parameterKey.substring(0, index);
                    if (ignoredParameter.test(jsonName)) {
                        continue;
                    }
                    fieldAttribute = parameterKey.substring(index + separator.length());
                }
            }
            if (values.isEmpty()) {
                throw new IllegalArgumentException("No values provided for " + parameterKey);
            }
            if (values.size() != 1) {
                throw new GetException("Cannot have " + parameterKey + " twice: Feature is not implemented");
            }

            parseSingleValueQueryParameter(jsonName, fieldAttribute, values.get(0));
            parsedParameters.computeIfAbsent(parameterKey, ignored -> new ArrayList<>()).addAll(values);
        }
    }

    protected void parseSingleValueQueryParameter(String jsonName, String fieldAttribute, String value) {
        String valueAttribute = null;
        if (extendedParameters.getQueryParameterAttributeHandling() == QueryParameterAttributeHandling.PARAMETER_VALUE_PREFIX) {
            String separator = extendedParameters.getQueryParameterAttributeSeparator();
            int index = value.indexOf(separator);
            if (index > -1) {
                valueAttribute = value.substring(0, index);
                value = value.substring(index + separator.length());
            }
        }
        parseSingleValueQueryParameter(getQueryFieldFromJSONName(jsonName), fieldAttribute, valueAttribute, value);
    }

    protected Expression bindValue(QueryField queryField, Object value) {
        referencedFields.add(queryField);
        MutableBindings bindings = getMutableBindings();
        BindMarker marker = bindings.nextMarker(queryField.getJsonName());
        bindings.bind(marker, value);
        return SQL.bindMarker(marker.getPlaceholder());
    }

    protected boolean handleIsNullQuery(QueryField queryField, String attribute, ComparisonType comparisonType, String value) {
        if ("NULL".equals(value)) {
            if (attribute == null) {
                comparisonType = ComparisonType.EQ;
            } else if (comparisonType != ComparisonType.EQ && comparisonType != ComparisonType.NEQ) {
                throw new GetException("Cannot use attribute (operator) " + attribute + " on " + queryField.getJsonName()
                        + " with null value: Null values can only be used with strictly equal or not equal");
            }
            Condition condition = Conditions.isNull(comparisonType.defaultFieldConversion(queryField));
            if (comparisonType == ComparisonType.NEQ) {
                condition = condition.not();
            }
            filters.add(InlineableFilterCondition.of(condition));
            return true;
        }
        return false;
    }

    protected void parseSingleValueQueryParameter(QueryField queryField, String fieldAttribute, String valueAttribute, String value) {
        Class<?> fieldType = queryField.getType();
        ComparisonType comparisonType = ComparisonType.EQ;
        String attribute = valueAttribute != null ? valueAttribute : fieldAttribute;
        Object parsed;
        if (attribute != null) {
            try {
                comparisonType = ComparisonType.valueOf(attribute.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new GetException("Attribute (operator) " + attribute + " is invalid for " + queryField.getJsonName());
            }
        }
        if (handleIsNullQuery(queryField, attribute, comparisonType, value)) {
            return;
        }

        if (fieldType.isEnum()) {
            // Return type IS Enum - split a possible list on EnumSplitter (since this is an Enum - no special characters are allowed). If
            // enumSplit value is a "," or a "|" this will work fine
            String[] enumList = value.split(extendedParameters.getEnumSplit());
            if (fieldAttribute == null) {
                comparisonType = ComparisonType.EQ;
            }
            if (comparisonType.isRequiredOrdering()) {
                throw new GetException("Cannot use attribute (operator) " + attribute + " on " + queryField.getJsonName()
                        + ": It does not have an ordering but the operator needs ordering");
            }
            if (enumList.length == 1) {
                filters.add(comparisonType.singleNonNullValueCondition(queryField, bindValue(queryField, enumList[0])));
            } else {
                filters.add(comparisonType.multiValueCondition(queryField,
                        Arrays.stream(enumList).map(v -> bindValue(queryField, v)).collect(Collectors.toList())
                ));
            }
            return;
        }

        if (String.class.equals(fieldType)) {
            parsed = value;
        } else if (UUID.class.equals(fieldType)) {
            if (comparisonType.isRequiredOrdering()) {
                throw new GetException("Cannot use attribute (operator) " + attribute + " on " + queryField.getJsonName()
                        + ": It does not have an ordering but the operator needs ordering");
            }
            if (attribute == null) {
                comparisonType = ComparisonType.EQ;
            } else if (comparisonType != ComparisonType.EQ) {
                throw new GetException("Cannot use attribute (operator) " + attribute + " on " + queryField.getJsonName()
                        + ": UUID values can only be used with strictly equal");
            }
            try {
                parsed = UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                throw new GetException("Cannot filter on " + queryField.getJsonName() + ": The value \"" + value
                        + "\" could not be parsed as an UUID");
            }
        } else if (Temporal.class.isAssignableFrom(fieldType)) {
            String dateFormat = queryField.getDatePattern();
            if (dateFormat != null) {
                throw new GetException("Cannot filter on " + queryField.getJsonName() + ": It uses a custom date pattern");
            }

            try {
                if (OffsetDateTime.class.equals(fieldType)) {
                    parsed = OffsetDateTime.parse(value);
                } else if (LocalDate.class.equals(fieldType)) {
                    parsed = LocalDate.parse(value);
                } else if (LocalDateTime.class.equals(fieldType)) {
                    parsed = LocalDateTime.parse(value);
                } else {
                    throw new GetException("Cannot filter on " + queryField.getJsonName() + ": Unsupported Temporal class");
                }
            } catch (DateTimeParseException e) {
                throw new GetException("Cannot filter on " + queryField.getJsonName() + ": The value \"" + value
                        + "\" could not be parsed in the correct date format");
            }
        } else if (Number.class.isAssignableFrom(fieldType)) {
            if (Long.class.equals(fieldType)) {
                parsed = Long.parseLong(value);
            } else if (Integer.class.equals(fieldType)) {
                parsed = Integer.parseInt(value);
            } else if (BigDecimal.class.equals(fieldType)) {
                parsed = new BigDecimal(value);
            } else {
                throw new GetException("Cannot filter on " + queryField.getJsonName()
                        + ": Numeric format " + fieldType.getTypeName() + " is not supported");
            }
        } else if (Boolean.class.equals(fieldType)) {
            if ("TRUE".equalsIgnoreCase(value)) {
                parsed = Boolean.TRUE;
            } else if ("FALSE".equalsIgnoreCase(value)) {
                parsed = Boolean.FALSE;
            } else {
                throw new GetException("Boolean filter value must be either: (TRUE|FALSE) - value not recognized: " + value + " on filter: " + fieldType.getSimpleName());
            }
        } else {
            throw new GetException("Type on filter (" + queryField.getJsonName() + ") not recognized: " + fieldType.getSimpleName());
        }
        filters.add(comparisonType.singleNonNullValueCondition(queryField, bindValue(queryField, parsed)));
    }

    protected QueryField getQueryFieldFromJSONName(String jsonName) {
        QueryField queryField;
        Field javaField;
        try {
            queryField = dbAnalysis.getQueryFieldFromJSONName(jsonName);
        } catch (IllegalArgumentException e) {
            throw new GetException("Unknown parameter/field name: " + jsonName);
        }
        javaField = queryField.getCombinedModelField();
        if (javaField != null) {
            if (javaField.isAnnotationPresent(JsonIgnore.class)) {
                throw new GetException("Cannot filter on field: " + jsonName + " (field is ignored)");
            }
            if (javaField.isAnnotationPresent(Transient.class)) {
                throw new GetException("Cannot filter on field: " + jsonName + " (field is transient)");
            }
        }
        referencedFields.add(queryField);
        return queryField;
    }


}
