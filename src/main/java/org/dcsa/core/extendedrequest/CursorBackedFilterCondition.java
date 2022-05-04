package org.dcsa.core.extendedrequest;

import org.springframework.data.relational.core.sql.OrderByField;
import org.springframework.r2dbc.core.binding.Bindings;

import java.util.List;
import java.util.Map;

public interface CursorBackedFilterCondition extends FilterCondition {

  /**
   * @return An iterable of all the query fields referenced either via the condition or other means
   *         (e.g., inside the {@link #getOrderByFields()}).
   */
  Iterable<QueryField> getReferencedQueryFields();

  Bindings getBindings();

  List<OrderByField> getOrderByFields();

  Map<String, List<String>> getCursorParameters();

  /**
   *
   * @return 0 if unlimited, otherwise a positive integer determining the max number of entities (SQL LIMIT)
   */
  int getLimit();

  /**
   *
   * @return 0 if no offset, otherwise a positive integer determining the offset (for OFFSET based queries)
   */
  int getOffset();
}
