package org.dcsa.core.extendedrequest;

import org.springframework.data.relational.core.sql.*;

public interface JoinDescriptor {

    Join.JoinType getJoinType();
    String getDependentAlias();
    Column getLHSColumn();
    Column getRHSColumn();

    default Class<?> getRHSModel() {
        return null;
    }

    default Table getRHSTable() {
        Column rhs = getRHSColumn();
        Table t = rhs.getTable();
        if (t == null) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " has a RHS Column without a table." +
                    " It needs to provide its own getRHSTable()");
        }
        return t;
    }

    default String getJoinAliasId() {
        Table t = getRHSTable();
        // Use IdentifierProcessing.NONE to ensure we get the original case of the alias.
        if (t instanceof Aliased) {
            return ((Aliased) t).getAlias().getReference(IdentifierProcessing.NONE);
        }
        return t.getReferenceName().getReference(IdentifierProcessing.NONE);
    }

}
