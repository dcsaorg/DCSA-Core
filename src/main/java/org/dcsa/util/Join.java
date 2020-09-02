package org.dcsa.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to help managing joins for the sql result.
 */
public class Join {
    List<String> joins = new ArrayList<>();

    public void add(String join) {
        joins.add(join);
    }

    protected void getJoinQueryString(StringBuilder sb) {
        for (String join: joins) {
            sb.append(" JOIN ");
            sb.append(join);
        }
    }
}
