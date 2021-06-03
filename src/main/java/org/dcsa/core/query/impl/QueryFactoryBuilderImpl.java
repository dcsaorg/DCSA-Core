package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.CursorBackedFilterCondition;
import org.dcsa.core.extendedrequest.JoinDescriptor;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.extendedrequest.TableAndJoins;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.query.QueryFactoryBuilder;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.dialect.RenderContextFactory;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SelectBuilder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor(staticName = "of")
public class QueryFactoryBuilderImpl<T> implements QueryFactoryBuilder<T> {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final DBEntityAnalysis<T> dbEntityAnalysis;
    private final boolean selectDistinct;

    private final Function<SelectBuilder.SelectFromAndJoin, SelectBuilder.SelectFromAndJoin> limitOffset;
    private final Function<SelectBuilder.SelectOrdered, SelectBuilder.BuildSelect> order;
    private final R2dbcDialect r2dbcDialect;

    public QueryFactoryBuilder<T> copyBuilder() {
        // Immutable, so we can just return this
        return this;
    }

    public QueryFactoryBuilder<T> distinct() {
        if (selectDistinct) {
            return this;
        }
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, true, limitOffset, order, r2dbcDialect);
    }

    public QueryFactoryBuilder<T> limitOffset(Function<SelectBuilder.SelectFromAndJoin, SelectBuilder.SelectFromAndJoin> limitOffsetFunction) {
        if (limitOffsetFunction == limitOffset) {
            return this;
        }
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, selectDistinct, limitOffsetFunction, order, r2dbcDialect);
    }

    public QueryFactoryBuilder<T> order(Function<SelectBuilder.SelectOrdered, SelectBuilder.BuildSelect> orderFunction) {
        if (orderFunction == order) {
            return this;
        }
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, selectDistinct, limitOffset, orderFunction, r2dbcDialect);
    }

    public QueryFactoryBuilder<T> r2dbcDialect(R2dbcDialect newR2dbcDialect) {
        if (newR2dbcDialect == this.r2dbcDialect) {
            return this;
        }
        return QueryFactoryBuilderImpl.of(dbEntityAnalysis, selectDistinct, limitOffset, order, newR2dbcDialect);
    }

    public AbstractQueryFactory<T> build(CursorBackedFilterCondition filterCondition) {
        if (r2dbcDialect == null) {
            throw new IllegalStateException("Missing r2dbcDialect parameter");
        }
        Condition condition = filterCondition.computeCondition(r2dbcDialect);
        Set<String> usedAliases = new HashSet<>();
        TableAndJoins tableAndJoins = dbEntityAnalysis.getTableAndJoins();
        List<Expression> expressions = dbEntityAnalysis.getAllSelectableFields().stream().map(queryField -> {
            addAliases(usedAliases, queryField, tableAndJoins);
            return queryField.getSelectColumn();
        }).collect(Collectors.toList());
        for (QueryField queryField : filterCondition.getReferencedQueryFields()) {
            addAliases(usedAliases, queryField, tableAndJoins);
        }
        return GenericQueryFactory.of(
                dbEntityAnalysis,
                selectDistinct,
                limitOffset,
                order,
                Set.of(usedAliases.toArray(EMPTY_STRING_ARRAY)),
                condition,
                expressions,
                new RenderContextFactory(r2dbcDialect),
                filterCondition.getBindings()
        );
    }

    private static void addAliases(Set<String> joinAliasInUse, QueryField fieldInUse, TableAndJoins tableAndJoins) {
        String joinAlias = fieldInUse.getTableJoinAlias();
        while (joinAlias != null) {
            JoinDescriptor descriptor = tableAndJoins.getJoinDescriptor(joinAlias);
            if (descriptor == null) {
                break;
            }
            String alias = descriptor.getJoinAliasId();
            if (!joinAliasInUse.add(alias)) {
                break;
            }
            joinAlias = descriptor.getDependentAlias();
        }
    }
}
