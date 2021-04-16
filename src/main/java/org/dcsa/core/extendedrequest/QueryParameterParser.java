package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.exception.GetException;
import org.dcsa.core.query.DBEntityAnalysis;
import org.springframework.data.annotation.Transient;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.*;
import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.Bindings;
import org.springframework.r2dbc.core.binding.MutableBindings;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class QueryParameterParser<T> {

    public static final FilterCondition EMPTY_CONDITION = InlineableFilterCondition.of(TrueCondition.INSTANCE);

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
            String parameterKey = queryParameter.getKey();
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
                    fieldAttribute = parameterKey.substring(0, index - 1);
                    parameterKey = parameterKey.substring(index + 1);
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
                valueAttribute = value.substring(0, index - 1);
                value = value.substring(index + 1);
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

    private static FilterCondition isubstr(Expression lhs, Expression rhsOrig) {
        Expression rhs = surroundWithWildCards(rhsOrig);
        return r2dbcDialect -> {
            if (r2dbcDialect instanceof PostgresDialect) {
                return Comparison.create(lhs, "ILIKE", rhs);
            }
            return Conditions.like(Functions.upper(lhs), Functions.upper(rhs));
        };
    }

    private static FilterCondition iequal(Expression lhs, Expression rhs) {
        return InlineableFilterCondition.of(Conditions.isEqual(Functions.upper(lhs), Functions.upper(rhs)));
    }

    private static Expression surroundWithWildCards(Expression rhs) {
        List<Expression> params = new ArrayList<>(3);
        params.add(SQL.literalOf("%"));
        params.add(rhs);
        params.add(SQL.literalOf("%"));
        return SimpleFunction.create("CONCAT", params);
    }

    @Getter
    @RequiredArgsConstructor
    protected enum ComparisonType {
        GT(true, Conditions::isGreater),
        GTE(true, Conditions::isGreaterOrEqualTo),
        EQ(false, Conditions::isEqual),
        LTE(true, Conditions::isLessOrEqualTo),
        LT(true, Conditions::isLess),
        NEQ(false, Conditions::isNotEqual),
        SUBSTR(false, Conditions::like),
        IEQ(false, true, QueryParameterParser::iequal),
        ISUBSTR(false, true, QueryParameterParser::isubstr),
        ;

        private final boolean requiredOrdering;
        private final boolean convertToString;
        private final BiFunction<Expression, Expression, FilterCondition> singleValueConverter;

        ComparisonType(boolean requiredOrdering, BiFunction<Expression, Expression, Condition> conditionBiFunction) {
            this(requiredOrdering, false, (lhs, rhs) -> InlineableFilterCondition.of(conditionBiFunction.apply(lhs, rhs)));
        }

        public Expression defaultFieldConversion(QueryField queryField) {
            boolean needsCast = convertToString;
            if (needsCast) {
                Class<?> valueType = queryField.getType();
                if (valueType.isEnum() || String.class.equals(valueType)) {
                    needsCast = false;
                }
            }
            if (needsCast) {
                Column aliased = queryField.getInternalQueryColumn().as(SqlIdentifier.unquoted("VARCHAR"));
                return SimpleFunction.create("CAST", Collections.singletonList(aliased));
            }
            return queryField.getInternalQueryColumn();
        }

        public FilterCondition singleNonNullValueCondition(QueryField queryField, Expression expression) {
            return singleNonNullValueCondition(defaultFieldConversion(queryField), expression);
        }

        public FilterCondition singleNonNullValueCondition(Expression field, Expression expression) {
            return singleValueConverter.apply(field, expression);
        }

        public FilterCondition multiValueCondition(QueryField queryField, List<Expression> expressions) {
            return multiValueCondition(defaultFieldConversion(queryField), expressions);
        }

        public FilterCondition multiValueCondition(Expression field, List<Expression> expressions) {
            if (expressions.isEmpty()) {
                throw new IllegalArgumentException("Right-hand side expression list must be non-empty");
            }
            if (expressions.size() == 1) {
                return singleNonNullValueCondition(field, expressions.get(0));
            } else if (this == EQ || this == NEQ) {
                if (this == EQ) {
                    return InlineableFilterCondition.of(Conditions.in(field, expressions));
                }
                return InlineableFilterCondition.of(Conditions.notIn(field, expressions));
            } else {
                return andAllFilters(
                        expressions.stream().map(e -> singleValueConverter.apply(field, e)).collect(Collectors.toList()),
                        true
                );
            }
        }
    }

    private static FilterCondition andAllFilters(List<FilterCondition> filters, boolean nestOnMultiple) {
        if (filters.isEmpty()) {
            return EMPTY_CONDITION;
        }
        if (filters.size() == 1) {
            return filters.get(0);
        }
        if (filters.stream().allMatch(FilterCondition::isInlineable)) {
            return InlineableFilterCondition.of(andAll(filters.stream().map(FilterCondition::computeCondition), nestOnMultiple));
        }
        return (r2dbcDialect) -> andAll(filters.stream().map(f -> f.computeCondition(r2dbcDialect)), nestOnMultiple);
    }

    private static Condition andAll(Stream<Condition> conditions, boolean nestOnMultiple) {
        return andAll(conditions::iterator, nestOnMultiple);
    }

    private static Condition andAll(Iterable<Condition> conditions, boolean nestOnMultiple) {
        Iterator<Condition> iter = conditions.iterator();
        assert iter.hasNext();
        Condition con = iter.next();
        if (!iter.hasNext()) {
            return con;
        }
        do {
            con = con.and(iter.next());
        } while (iter.hasNext());
        if (!nestOnMultiple) {
            return con;
        }
        return Conditions.nest(con);
    }

    @RequiredArgsConstructor(staticName = "of")
    private static class InlineableFilterCondition implements FilterCondition {
        private final Condition condition;

        public Condition computeCondition(R2dbcDialect r2dbcDialect) {
            return condition;
        }

        public boolean isInlineable() {
            return true;
        }
    }

    @RequiredArgsConstructor(staticName = "of")
    private static class DelegatingCursorBackedFilterCondition implements CursorBackedFilterCondition {

        private final FilterCondition delegate;

        @Getter
        private final Map<String, List<String>> cursorParameters;

        @Getter
        private final Set<QueryField> referencedQueryFields;

        @Getter
        private final Bindings bindings;

        @Override
        public Condition computeCondition(R2dbcDialect r2dbcDialect) {
            return delegate.computeCondition(r2dbcDialect);
        }

        @Override
        public boolean isInlineable() {
            return delegate.isInlineable();
        }
    }
}
