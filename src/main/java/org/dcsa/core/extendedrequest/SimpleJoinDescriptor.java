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

    private final String dependentAlias;
}
