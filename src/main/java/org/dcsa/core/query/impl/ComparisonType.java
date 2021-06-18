package org.dcsa.core.query.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.FilterCondition;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.extendedrequest.QueryParameterParser;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.relational.core.sql.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.dcsa.core.query.impl.QueryGeneratorUtils.andAllFilters;

@Getter
@RequiredArgsConstructor
public enum ComparisonType {
    GT(true, Conditions::isGreater),
    GTE(true, Conditions::isGreaterOrEqualTo),
    EQ(false, Conditions::isEqual),
    LTE(true, Conditions::isLessOrEqualTo),
    LT(true, Conditions::isLess),
    NEQ(false, Conditions::isNotEqual),
    SUBSTR(false, Conditions::like),
    IEQ(false, true, ComparisonType::iequal),
    ISUBSTR(false, true, ComparisonType::isubstr),
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
}