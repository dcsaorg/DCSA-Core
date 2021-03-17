package org.dcsa.core.extendedrequest;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.model.JoinedWithModel;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Join;

@RequiredArgsConstructor(staticName = "of")
@Data
public class JoinedWithModelBasedJoinDescriptor implements JoinDescriptor {

    private final Join.JoinType joinType;

    @NonNull
    private final Column LHSColumn;

    @NonNull
    private final Column RHSColumn;

    private final String dependentAlias;

    @NonNull
    private final JoinedWithModel joinedWithModel;
    private final Class<?> modelClass;

}
