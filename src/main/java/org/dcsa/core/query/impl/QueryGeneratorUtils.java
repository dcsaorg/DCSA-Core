package org.dcsa.core.query.impl;

import org.dcsa.core.extendedrequest.FilterCondition;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.TrueCondition;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class QueryGeneratorUtils {

    public static final FilterCondition EMPTY_CONDITION = InlineableFilterCondition.of(TrueCondition.INSTANCE);

    public static FilterCondition andAllFilters(List<FilterCondition> filters, boolean nestOnMultiple) {
        if (filters.isEmpty()) {
            return EMPTY_CONDITION;
        }
        if (filters.size() == 1) {
            return filters.get(0);
        }
        if (filters.stream().allMatch(FilterCondition::isInlineable)) {
            return InlineableFilterCondition.of(andAll(filters.stream().map(FilterCondition::computeCondition), nestOnMultiple));
        }
        return (r2dbcDialect) -> andAll(filters.stream().map(f -> f.computeCondition(r2dbcDialect)), nestOnMultiple);
    }

    public static Condition andAll(Stream<Condition> conditions, boolean nestOnMultiple) {
        return andAll(conditions::iterator, nestOnMultiple);
    }

    public static Condition andAll(Iterable<Condition> conditions, boolean nestOnMultiple) {
        Iterator<Condition> iter = conditions.iterator();
        assert iter.hasNext();
        Condition con = iter.next();
        if (!iter.hasNext()) {
            return con;
        }
        do {
            con = con.and(iter.next());
        } while (iter.hasNext());
        if (!nestOnMultiple) {
            return con;
        }
        return Conditions.nest(con);
    }



}
