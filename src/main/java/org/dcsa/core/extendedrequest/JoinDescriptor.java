package org.dcsa.core.extendedrequest;

import org.springframework.data.relational.core.sql.Join;

public interface JoinDescriptor {

    Join.JoinType getJoinType();
    String getTableName();
    String getJoinAlias();
    String getJoinCondition();
    String getDependentAlias();

    default void apply(Joiner joiner) {
        String tableName = getTableName();
        String joinAlias = getJoinAlias();
        String joinCondition = getJoinCondition();
        String joinLine = tableName + " AS " + joinAlias + " " + joinCondition;
        joiner.addJoin(this, joinLine);
    }
}
