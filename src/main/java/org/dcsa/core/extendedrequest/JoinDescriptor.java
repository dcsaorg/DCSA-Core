package org.dcsa.core.extendedrequest;

import org.springframework.data.relational.core.sql.*;

public interface JoinDescriptor {

    Join.JoinType getJoinType();
    String getDependentAlias();
    Condition getCondition();

    default Class<?> getRHSModel() {
        return null;
    }

    TableLike getRHSTable();

    default String getJoinAliasId() {
        TableLike t = getRHSTable();
        // Use IdentifierProcessing.NONE to ensure we get the original case of the alias.
        if (t instanceof Aliased) {
            return ((Aliased) t).getAlias().getReference(IdentifierProcessing.NONE);
        }
        return t.getReferenceName().getReference(IdentifierProcessing.NONE);
    }

}
