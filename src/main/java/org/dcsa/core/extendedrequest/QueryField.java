package org.dcsa.core.extendedrequest;

import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;

public interface QueryField {

  String getJsonName();
  String getTableJoinAlias();
  Column getSelectColumn();
  Column getInternalQueryColumn();
  Class<?> getType();

  default boolean isSelectable() {
    return getSelectColumn() != null;
  }

  default String getDatePattern() {
    return null;
  }
  default Field getCombinedModelField() {
    return null;
  }

  /**
   * Generates the condition for this query field for a concrete request (optional operation)
   *
   * <p>
   * This method will be called whenever the query field is used as a query parameter.  Note it can be called multiple
   * times for a given request if the field attributes differ and the results will be AND'ed together.
   * <p>
   * The implementation should convert the input into a {@link FilterCondition} using the following over all steps:
   *
   * <ol>
   *     <li>The implementation should check it is defined for the given comparison type. If not, throw an exception
   *         that will cause an HTTP Bad Request error.</li>
   *     <li>All values from queryParamValues that go into the query should be passed through the value2BindVariable
   *         function to get the bind variable to insert the value there. The string value passed there must be
   *         convertable into the underlying type of this query field.</li>
   *     <li>The bind variable(s) from the previous step should be passed to methods like
   *     {@link ComparisonType#singleNonNullValueCondition(QueryField, Expression)} or
   *     {@link ComparisonType#multiValueCondition(QueryField, List)} to generate the condition</li>
   *
   * </ol>
   *
   * @param comparisonType     The comparison type used (defaults to {@link ComparisonType#EQ}). The code is not required to use this
   *                           comparison type - it is just what the engine thought would be used in the default context.
   *                           This generally mirrors the fieldAttribute except you cannot tell whether the choice was explicit
   *                           or implicit.
   *                           If the method does not support the given comparison type it should throw an exception that will
   *                           cause an HTTP 400 Bad Request response.
   * @param fieldAttribute     The field attribute used on the attribute (defaults to null when there is no explicit attribute).
   *                           This uses the exact case as provided by the client.
   * @param queryParamValues   The values passed as query parameters (as-is) for this choice of field attribute.  The code
   *                           may preprocess the values however it likes (but it may need to copy the list to do so as
   *                           the list itself is immutable).
   * @param value2BindVariable A function that maps a single query parameter value into the underlying type needed in the database.
   * @return The filterCondition that represents the SQL condition (or null, if the default generator should be used).
   */
  default FilterCondition generateCondition(ComparisonType comparisonType,
                                            String fieldAttribute,
                                            List<String> queryParamValues,
                                            Function<String, Expression> value2BindVariable
  ) {
    return null;
  }
}
