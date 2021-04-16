package org.dcsa.core.extendedrequest;

import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.Condition;

public interface FilterCondition {

    Condition computeCondition(R2dbcDialect r2dbcDialect);

    default Condition computeCondition() {
        return computeCondition(null);
    };

    default boolean isInlineable() {
        return false;
    }
}
