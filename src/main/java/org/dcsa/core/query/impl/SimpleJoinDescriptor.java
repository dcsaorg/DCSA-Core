package org.dcsa.core.query.impl;

import lombok.Data;
import lombok.NonNull;
import org.dcsa.core.extendedrequest.JoinDescriptor;
import org.springframework.data.relational.core.sql.*;

@Data(staticConstructor = "of")
class SimpleJoinDescriptor implements JoinDescriptor {

  @NonNull
  private final Join.JoinType joinType;

  private final TableLike RHSTable;

  private final Class<?> RHSModel;

  private final Condition condition;

  private final String dependentAlias;

  public static JoinDescriptor of(Join.JoinType joinType, Column lhsColumn, Column rhsColumn, String dependentAlias) {
    return of(joinType, rhsColumn.getTable(), null, Conditions.isEqual(lhsColumn, rhsColumn), dependentAlias);
  }
}
