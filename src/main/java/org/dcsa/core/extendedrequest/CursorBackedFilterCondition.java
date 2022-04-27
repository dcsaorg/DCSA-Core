package org.dcsa.core.extendedrequest;

import org.springframework.r2dbc.core.binding.Bindings;

import java.util.List;
import java.util.Map;

public interface CursorBackedFilterCondition extends FilterCondition {

  Iterable<QueryField> getReferencedQueryFields();

  Bindings getBindings();

  Map<String, List<String>> getCursorParameters();
}
