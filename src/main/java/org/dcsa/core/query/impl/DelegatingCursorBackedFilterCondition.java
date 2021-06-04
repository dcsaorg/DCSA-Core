package org.dcsa.core.query.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.CursorBackedFilterCondition;
import org.dcsa.core.extendedrequest.FilterCondition;
import org.dcsa.core.extendedrequest.QueryField;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.r2dbc.core.binding.Bindings;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor(staticName = "of")
public class DelegatingCursorBackedFilterCondition implements CursorBackedFilterCondition {

    private final FilterCondition delegate;

    @Getter
    private final Map<String, List<String>> cursorParameters;

    @Getter
    private final Set<QueryField> referencedQueryFields;

    @Getter
    private final Bindings bindings;

    @Override
    public Condition computeCondition(R2dbcDialect r2dbcDialect) {
        return delegate.computeCondition(r2dbcDialect);
    }

    @Override
    public boolean isInlineable() {
        return delegate.isInlineable();
    }
}
