package org.dcsa.core.extendedrequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.sql.*;

import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
enum ComparisonType {
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
        this(requiredOrdering, false, (lhs, rhs) -> QueryParameterParser.InlineableFilterCondition.of(conditionBiFunction.apply(lhs, rhs)));
    }

    public Expression defaultFieldConversion(QueryField queryField) {
        Class<?> valueType = queryField.getType();
        if (convertToString && !(valueType.isEnum() || String.class.equals(valueType))) {
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
                return QueryParameterParser.InlineableFilterCondition.of(Conditions.in(field, expressions));
            }
            return QueryParameterParser.InlineableFilterCondition.of(Conditions.notIn(field, expressions));
        } else {
            return QueryParameterParser.andAllFilters(
                    expressions.stream().map(e -> singleValueConverter.apply(field, e)).collect(Collectors.toList()),
                    true
            );
        }
    }
}
