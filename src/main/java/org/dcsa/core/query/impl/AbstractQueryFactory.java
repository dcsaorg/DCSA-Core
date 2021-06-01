package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.TableAndJoins;
import org.dcsa.core.query.DBEntityAnalysis;
import org.springframework.data.relational.core.sql.*;
import org.springframework.r2dbc.core.PreparedOperation;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public abstract class AbstractQueryFactory<T> {

    public abstract DBEntityAnalysis<T> getDbEntityAnalysis();

    protected abstract Set<String> getJoinAliasInUse();

    protected abstract boolean isSelectDistinct();

    protected abstract SelectBuilder.SelectFromAndJoin applyLimitOffset(SelectBuilder.SelectFromAndJoin t);

    protected SelectBuilder.SelectOrdered generateBaseQuery(SelectBuilder.SelectAndFrom selectBuilder) {
        DBEntityAnalysis<T> dbEntityAnalysis = getDbEntityAnalysis();
        TableAndJoins tableAndJoins = dbEntityAnalysis.getTableAndJoins();
        // Run this before the call to applyJoins(...) as it can affect which joins will be used.
        Condition con = generateCondition();

        if (isSelectDistinct()) {
            selectBuilder = selectBuilder.distinct();
        }
        SelectBuilder.SelectWhere selectWhere = applyJoins(tableAndJoins, applyLimitOffset(selectBuilder.from(
                tableAndJoins.getPrimaryTable()
        )));
        if (con == null || TrueCondition.INSTANCE.equals(con)) {
            return selectWhere;
        }
        return selectWhere.where(con);
    }

    protected abstract Condition generateCondition();

    protected abstract SelectBuilder.BuildSelect applyOrder(SelectBuilder.SelectOrdered builder);

    public abstract PreparedOperation<Select> generateSelectQuery();

    protected PreparedOperation<Select> generateSelectQuery(List<Expression> expressions) {
        return createPreparedOperation(applyOrder(generateBaseQuery(Select.builder().select(expressions))).build());
    }

    public PreparedOperation<Select> generateCountQuery() {
        return createPreparedOperation(generateBaseQuery(Select.builder().select(
                Functions.count(Expressions.asterisk()).as("count")
        )).build());
    }

    protected abstract PreparedOperation<Select> createPreparedOperation(Select select);

    private SelectBuilder.SelectWhere applyJoins(TableAndJoins tableAndJoins, SelectBuilder.SelectFromAndJoin selectBuilder) {
        Set<String> joinAliasInUse = getJoinAliasInUse();
        if (!joinAliasInUse.isEmpty()) {
            return tableAndJoins.applyJoins(selectBuilder, joinAliasInUse);
        }
        return selectBuilder;
    }
}
