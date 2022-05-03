package org.dcsa.core.extendedrequest;

import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.Condition;

@RequiredArgsConstructor(staticName = "of")
class InlineableFilterCondition implements FilterCondition {
  private final Condition condition;

  public Condition computeCondition(R2dbcDialect r2dbcDialect) {
    return condition;
  }

  public boolean isInlineable() {
    return true;
  }
}
