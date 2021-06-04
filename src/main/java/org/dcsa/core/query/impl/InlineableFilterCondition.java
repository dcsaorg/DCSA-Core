package org.dcsa.core.query.impl;

import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.FilterCondition;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.Condition;

@RequiredArgsConstructor(staticName = "of")
public class InlineableFilterCondition implements FilterCondition {
    private final Condition condition;

    public Condition computeCondition(R2dbcDialect r2dbcDialect) {
        return condition;
    }

    public boolean isInlineable() {
        return true;
    }
}
