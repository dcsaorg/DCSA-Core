package org.dcsa.core.extendedrequest;

public interface JoinDescriptor {

    org.springframework.data.relational.core.sql.Join.JoinType getJoinType();
    String getTableName();
    String getJoinAlias();
    String getJoinCondition();
    String getDependentAlias();
    boolean isInUse();
    void setInUse(boolean inUse);

    default void apply(Join join) {
        String tableName = getTableName();
        String joinAlias = getJoinAlias();
        String joinCondition = getJoinCondition();
        String joinLine = tableName + " AS " + joinAlias + " " + joinCondition;
        org.springframework.data.relational.core.sql.Join.JoinType joinType = getJoinType();
        switch (joinType) {
            case JOIN:
                join.doInner(joinLine);
                break;
            case LEFT_OUTER_JOIN:
                join.doLeft(joinLine);
                break;
            case RIGHT_OUTER_JOIN:
                join.doRight(joinLine);
                break;
            default:
                throw new IllegalArgumentException("Unsupported joinType: " + joinType.name());
        }
    }
}
