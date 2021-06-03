package org.dcsa.core.query;

import org.dcsa.core.extendedrequest.CursorBackedFilterCondition;
import org.dcsa.core.query.impl.AbstractQueryFactory;
import org.dcsa.core.query.impl.QueryFactoryBuilderImpl;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.SelectBuilder;

import java.util.function.Function;

public interface QueryFactoryBuilder<T> {

    QueryFactoryBuilder<T> distinct();
    QueryFactoryBuilder<T> limitOffset(Function<SelectBuilder.SelectFromAndJoin, SelectBuilder.SelectFromAndJoin> limitOffsetFunction);
    QueryFactoryBuilder<T> order(Function<SelectBuilder.SelectOrdered, SelectBuilder.BuildSelect> orderFunction);
    QueryFactoryBuilder<T> r2dbcDialect(R2dbcDialect r2dbcDialect);
    QueryFactoryBuilder<T> copyBuilder();
    AbstractQueryFactory<T> build(CursorBackedFilterCondition filterCondition);

    static <T> QueryFactoryBuilder<T> builder(DBEntityAnalysis<T> dbEntityAnalysis) {
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, false, null, null, null);
    }
}
