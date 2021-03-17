package org.dcsa.core.extendedrequest;

import lombok.Getter;
import org.springframework.data.relational.core.sql.*;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * A class to help managing joins for the sql result.
 */
public class TableAndJoins {

    @Getter
    private final Table primaryTable;
    private final Set<String> knownAliases = new HashSet<>();
    private final LinkedHashMap<String, JoinDescriptor> joins = new LinkedHashMap<>();

    public TableAndJoins(Table primaryTable) {
        this.primaryTable = primaryTable;
        SqlIdentifier aliasId;
        if (primaryTable instanceof Aliased) {
            aliasId = ((Aliased) primaryTable).getAlias();
        } else {
            aliasId = primaryTable.getReferenceName();
        }
        knownAliases.add(aliasId.getReference(IdentifierProcessing.NONE));
    }

    public void addJoinDescriptor(JoinDescriptor joinDescriptor) {
        if (joinDescriptor.getDependentAlias() != null && !knownAliases.contains(joinDescriptor.getDependentAlias())) {
            throw new IllegalArgumentException("Cannot add joinDescriptor for " + joinDescriptor.getJoinAliasId()
                    + ": It must not be added before " + joinDescriptor.getDependentAlias() + ", which it depends on");
        }
        if (joins.putIfAbsent(joinDescriptor.getJoinAliasId(), joinDescriptor) != null) {
            throw new IllegalArgumentException(joinDescriptor.getJoinAliasId() + " used twice!");
        }
        knownAliases.add(joinDescriptor.getJoinAliasId());
    }

    public JoinDescriptor getJoinDescriptor(String aliasId) {
        return joins.get(aliasId);
    }

    private static void applyJoinToStringBuilder(StringBuilder sb, JoinDescriptor descriptor) {
        Column lhs = descriptor.getLHSColumn();
        Column rhs = descriptor.getRHSColumn();
        Table table = descriptor.getRHSTable();
        String joinLine = table.toString() + " ON " + lhs.toString() + "=" + rhs.toString();
        switch (descriptor.getJoinType()) {
            case JOIN:
                sb.append(" JOIN ");
                break;
            case LEFT_OUTER_JOIN:
                sb.append(" LEFT JOIN ");
                break;
            case RIGHT_OUTER_JOIN:
                throw new UnsupportedOperationException("Rewrite to use LEFT JOIN instead");
            default:
                throw new UnsupportedOperationException("Unsupported join type: " + descriptor.getJoinType());
        }
        sb.append(joinLine);
    }

    protected void generateFromAndJoins(StringBuilder sb, Set<String> selectedJoinAliases) {
        int selectedJoins = 0;
        sb.append(" FROM ");
        sb.append(primaryTable.toString());
        for (JoinDescriptor joinDescriptor : joins.values()) {
            if (selectedJoinAliases.contains(joinDescriptor.getJoinAliasId())) {
                applyJoinToStringBuilder(sb, joinDescriptor);
                selectedJoins++;
            }
        }
        if (selectedJoinAliases.size() != selectedJoins) {
            throw new IllegalArgumentException("selectedJoinAliases contained an unknown alias");
        }
    }

}
