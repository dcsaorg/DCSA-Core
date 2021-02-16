package org.dcsa.core.extendedrequest;

import lombok.Data;
import lombok.NonNull;

@Data(staticConstructor = "of")
public class SimpleJoinDescriptor implements JoinDescriptor {

    @NonNull
    private final org.springframework.data.relational.core.sql.Join.JoinType joinType;

    @NonNull
    private final String tableName;

    @NonNull
    private final String joinAlias;

    @NonNull
    private final String joinCondition;
    private final String dependentAlias;
    private boolean inUse = false;
}
