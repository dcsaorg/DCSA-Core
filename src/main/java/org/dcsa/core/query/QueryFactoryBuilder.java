package org.dcsa.core.query;

import org.apache.commons.lang3.concurrent.BackgroundInitializer;
import org.dcsa.core.query.impl.AbstractQueryFactory;
import org.dcsa.core.query.impl.QueryFactoryBuilderImpl;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.SelectBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface QueryFactoryBuilder<T> {

    QueryFactoryBuilder<T> distinct();
    QueryFactoryBuilder<T> limitOffset(Function<SelectBuilder.SelectFromAndJoin, SelectBuilder.SelectFromAndJoin> limitOffsetFunction);
    QueryFactoryBuilder<T> order(Function<SelectBuilder.SelectOrdered, SelectBuilder.BuildSelect> orderFunction);
    QueryFactoryBuilder<T> order(BiFunction<SelectBuilder.SelectOrdered, DBEntityAnalysis<T>, SelectBuilder.BuildSelect> orderFunction);
    QueryFactoryBuilder<T> r2dbcDialect(R2dbcDialect r2dbcDialect);
    FrozenBuilder<QueryFactoryBuilder<T>> freeze();

    ConditionBuilder<T> conditions();
    AbstractQueryFactory<T> build();

    static <T> QueryFactoryBuilder<T> builder(DBEntityAnalysis<T> dbEntityAnalysis) {
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, false, null, null, null, null);
    }

    interface ConditionBuilder<T> {

        ComparisonBuilder<T> fieldByJsonName(String jsonName);
        ComparisonBuilder<T> fieldByJavaField(String javaField);
        QueryFactoryBuilder<T> withQuery();

        FrozenBuilder<ConditionBuilder<T>> freeze();
        AbstractQueryFactory<T> build();
    }

    interface FrozenBuilder<B> {
        B copyBuilder();
    }

    interface ComparisonBuilder<T> {
        ConditionBuilder<T> isNull();
        ConditionBuilder<T> isNotNull();
        ConditionBuilder<T> greaterThan(Object value);
        ConditionBuilder<T> greaterThanOrEqualTo(Object value);
        ConditionBuilder<T> equalTo(Object value);
        ConditionBuilder<T> lessThanOrEqualTo(Object value);
        ConditionBuilder<T> lessThan(Object value);
        default ConditionBuilder<T> oneOf(Object ... values) {
            return oneOf(Arrays.asList(values));
        }
        ConditionBuilder<T> oneOf(List<Object> values);
    }
}
