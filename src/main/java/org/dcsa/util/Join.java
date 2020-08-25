package org.dcsa.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A class used to store which joins to use when extracting Collection results.
 */
public class Join {
    List<String> joins = new ArrayList<>();

    public void add(String join) {
        joins.add(join);
    }

    public String getJoin(int index) {
        return joins.get(index);
    }

    public int getSize() {
        return joins.size();
    }
}
