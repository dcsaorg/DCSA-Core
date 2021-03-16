package org.dcsa.core.extendedrequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A class to help managing joins for the sql result.
 */
public class Joiner {


    static class DefaultJoinHandler {
        protected final JoinDescriptor joinDescriptor;
        protected final String joinLine;

        public DefaultJoinHandler(JoinDescriptor joinDescriptor, String joinLine) {
            this.joinDescriptor = joinDescriptor;
            this.joinLine = joinLine;
        }

        public void doJoin(StringBuilder sb) {
            switch (joinDescriptor.getJoinType()) {
                case JOIN:
                    sb.append(" JOIN ");
                    break;
                case LEFT_OUTER_JOIN:
                    sb.append(" LEFT JOIN ");
                    break;
                case RIGHT_OUTER_JOIN:
                    throw new UnsupportedOperationException("Rewrite to use LEFT JOIN instead");
                default:
                    throw new UnsupportedOperationException("Unsupported join type: " + joinDescriptor.getJoinType());
            }
            sb.append(joinLine);
        }
    }

    private final List<DefaultJoinHandler> joins = new ArrayList<>();

    public void addJoin(JoinDescriptor joinDescriptor, String joinLine) {
        joins.add(new DefaultJoinHandler(joinDescriptor, joinLine));
    }

    protected void getJoinQueryString(StringBuilder sb, Set<String> selectedJoinAliases) {
        int selectedJoins = 0;
        for (DefaultJoinHandler join: joins) {
            if (selectedJoinAliases.contains(join.joinDescriptor.getJoinAlias())) {
                join.doJoin(sb);
                selectedJoins++;
            }
        }
        if (selectedJoinAliases.size() != selectedJoins) {
            throw new IllegalArgumentException("selectedJoinAliases contained an unknown alias");
        }
    }

}
