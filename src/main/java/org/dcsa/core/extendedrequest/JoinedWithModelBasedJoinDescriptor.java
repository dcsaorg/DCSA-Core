package org.dcsa.core.extendedrequest;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.model.JoinedWithModel;

@RequiredArgsConstructor(staticName = "of")
@Data
public class JoinedWithModelBasedJoinDescriptor implements JoinDescriptor {

    private final org.springframework.data.relational.core.sql.Join.JoinType joinType;

    @NonNull
    private final String tableName;

    @NonNull
    private final String joinAlias;

    @NonNull
    private final String joinCondition;

    private final String dependentAlias;

    @NonNull
    private final JoinedWithModel joinedWithModel;
    private final Class<?> modelClass;
    private boolean inUse = false;

}
