package org.dcsa.core.extendedrequest;

import lombok.Data;
import lombok.NonNull;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Join;

@Data(staticConstructor = "of")
public class SimpleJoinDescriptor implements JoinDescriptor {

    @NonNull
    private final Join.JoinType joinType;

    @NonNull
    private final Column LHSColumn;

    @NonNull
    private final Column RHSColumn;

    private final Class<?> RHSModel;

    private final String dependentAlias;

    public static JoinDescriptor of(Join.JoinType joinType, Column lhsColumn, Column rhsColumn, String dependentAlias) {
        return of(joinType, lhsColumn, rhsColumn, null, dependentAlias);
    }
}
