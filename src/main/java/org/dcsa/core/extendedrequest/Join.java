package org.dcsa.core.extendedrequest;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to help managing joins for the sql result.
 */
public class Join {

    abstract static class DefaultJoin {
        String join;

        public DefaultJoin(String join) {
            this.join = join;
        }

        abstract public void doJoin(StringBuilder sb);
    }

    static class InnerJoin extends DefaultJoin {
        public InnerJoin(String join) {
            super(join);
        }

        public void doJoin(StringBuilder sb) {
            sb.append(" JOIN ");
            sb.append(join);
        }
    }

    static class LeftJoin extends DefaultJoin {
        public LeftJoin(String join) {
            super(join);
        }

        @Override
        public void doJoin(StringBuilder sb) {
            sb.append(" LEFT JOIN ");
            sb.append(join);
        }
    }

    static class RightJoin extends DefaultJoin {
        public RightJoin(String join) {
            super(join);
        }

        @Override
        public void doJoin(StringBuilder sb) {
            sb.append(" RIGHT JOIN ");
            sb.append(join);
        }
    }

    List<DefaultJoin> joins = new ArrayList<>();

    public void doInner(String join) {
        joins.add(new InnerJoin(join));
    }
    public void doLeft(String join) {
        joins.add(new LeftJoin(join));
    }
    public void doRight(String join) {
        joins.add(new RightJoin(join));
    }

    protected void getJoinQueryString(StringBuilder sb) {
        for (DefaultJoin join: joins) {
            join.doJoin(sb);
        }
    }
}
