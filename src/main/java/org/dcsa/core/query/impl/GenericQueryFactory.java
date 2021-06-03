package org.dcsa.core.query.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.query.DBEntityAnalysis;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.binding.Bindings;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@RequiredArgsConstructor(staticName = "of")
class GenericQueryFactory<T> extends AbstractQueryFactory<T> {

    @Getter
    private final DBEntityAnalysis<T> dbEntityAnalysis;

    @Getter
    private final boolean selectDistinct;

    private final Function<SelectBuilder.SelectFromAndJoin, SelectBuilder.SelectFromAndJoin> limitOffsets;
    private final Function<SelectBuilder.SelectOrdered, SelectBuilder.BuildSelect> order;
    @Getter
    private final Set<String> joinAliasInUse;
    private final Condition conditions;
    private final List<Expression> selectExpressions;

    private final RenderContextFactory renderContextFactory;
    private final Bindings bindings;


    @Override
    public PreparedOperation<Select> generateSelectQuery() {
        return generateSelectQuery(selectExpressions);
    }

    @Override
    protected PreparedOperation<Select> createPreparedOperation(Select select) {
        return PreparedQuery.of(select, renderContextFactory.createRenderContext(), bindings);
    }

    @Override
    protected Condition generateCondition() {
        return conditions;
    }

    @Override
    protected SelectBuilder.SelectFromAndJoin applyLimitOffset(SelectBuilder.SelectFromAndJoin builder) {
        if (limitOffsets != null) {
            return limitOffsets.apply(builder);
        }
        return builder;
    }

    @Override
    protected SelectBuilder.BuildSelect applyOrder(SelectBuilder.SelectOrdered builder) {
        if (order != null) {
            return order.apply(builder);
        }
        return builder;
    }
}
