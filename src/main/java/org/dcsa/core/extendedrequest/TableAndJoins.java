package org.dcsa.core.extendedrequest;

import lombok.Getter;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.SelectBuilder;
import org.springframework.data.relational.core.sql.Table;

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
    private final String primaryTableAlias;

    public TableAndJoins(Table primaryTable) {
        this.primaryTable = primaryTable;
        primaryTableAlias = ReflectUtility.getAliasId(primaryTable);
        knownAliases.add(primaryTableAlias);
    }

    public void addJoinDescriptor(JoinDescriptor joinDescriptor) {
        if (joinDescriptor.getDependentAlias() != null && !joinDescriptor.getDependentAlias().equals("") && !knownAliases.contains(joinDescriptor.getDependentAlias())) {
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

    public boolean hasJoins() {
        return !joins.isEmpty();
    }

    public SelectBuilder.SelectFromAndJoinCondition applyJoins(SelectBuilder.SelectFromAndJoin selectFromAndJoin, Set<String> selectedJoinAliases) {
        SelectBuilder.SelectJoin current = selectFromAndJoin;
        int selectedJoins = 0;
        if (!hasJoins()) {
            throw new IllegalStateException("No joins to apply");
        }
        for (JoinDescriptor joinDescriptor: joins.values()) {
            if (selectedJoinAliases.contains(joinDescriptor.getJoinAliasId())) {
                current = applyJoin(joinDescriptor, current);
                selectedJoins++;
            }
        }
        // "Forgive" if the primary table is included as well.
        if (selectedJoinAliases.contains(primaryTableAlias)) {
            selectedJoins++;
        }
        if (selectedJoinAliases.size() != selectedJoins) {
            throw new IllegalArgumentException("selectedJoinAliases contained an unknown alias");
        }
        return (SelectBuilder.SelectFromAndJoinCondition)current;
    }


    private SelectBuilder.SelectFromAndJoinCondition applyJoin(JoinDescriptor joinDescriptor, SelectBuilder.SelectJoin selectBuilder) {
        SelectBuilder.SelectOn selectOn;
        Table table = joinDescriptor.getRHSTable();
        Expression lhsExpression = joinDescriptor.getLHSColumn();
        Expression rhsExpression = joinDescriptor.getRHSColumn();
        switch (joinDescriptor.getJoinType()) {
            case JOIN:
                selectOn = selectBuilder.join(table);
                break;
            case LEFT_OUTER_JOIN:
                selectOn = selectBuilder.leftOuterJoin(table);
                break;
            case RIGHT_OUTER_JOIN:
                throw new UnsupportedOperationException("Rewrite to use LEFT JOIN instead");
            default:
                throw new UnsupportedOperationException("Unsupported join type: " + joinDescriptor.getJoinType());
        }
        return selectOn.on(lhsExpression).equals(rhsExpression);
    }
}
