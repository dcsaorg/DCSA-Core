package org.dcsa.core.extendedrequest;

import org.dcsa.core.exception.ConcreteRequestErrorMessageException;
import org.springframework.data.relational.core.sql.OrderByField;

import java.util.List;

/**
 * A method for parsing a parameter.
 * <p>
 * The interface is used in a stateful manner, so all implementation must be a part of the
 * {@link QueryParameterParser} instance itself (so it can update parseState, etc.)
 */
interface ParserFunction {

  /**
   * Parse all values of a single parameter (with a given attribute)
   * <p>
   * The method must perform the relevant actions:
   * <ol>
   *   <li>Validate that the input is correct / well formed.  Errors should be raised as {@link ConcreteRequestErrorMessageException} exceptions</li>
   *   <li>Record the parameter as parsed if it should go into the cursor by invoking {@link QueryParameterParser#recordAttributeInCursor(String, List)}.
   *      (Only very special parameters such as the cursor itself and the internal "offset" tracker are exempt from recording)</li>
   *   <li>Do the actual parameter handling (e.g., sort is mapped to {@link OrderByField} and recorded inside the orderByFields field).</li>
   *   <li>Update the parser state.  Most cases want a {@code parseState = parseState.parsedArgument(extendedParameters.getPaginationCursorName);}
   *       to mark the parameter as parsed.  This state update is a part of enforcing that the cursor is not mixed with other parameters</li>
   * </ol>
   *
   * @param parameterKey   The original name of the parameter as provided by the client (e.g., "foo:gte")
   * @param jsonName       The json name of the parameter. This is the parameterKey minus the field attributes (e.g., "foo")
   * @param fieldAttribute The field attribute (without the separator) if present or null if absent. (e.g., "gte").
   *                       When a parameter cannot be used with a attribute (such as "limit"), the code can invoke
   *                       {@link QueryParameterParser#noFieldAttributeAllowed(String, String)} to enforce this
   *                       aspect.
   * @param values         The values associated with this attribute (parameterKey).  Each element represent the value from
   *                       a "foo:gte=value1" instance in the URL. ParserFunctions that require exactly one value can use
   *                       {@link QueryParameterParser#queryParameterHasExactlyOneValue(String, List)} to validate
   *                       this aspect.
   */
  void parseParameter(String parameterKey, String jsonName, String fieldAttribute, List<String> values);
}
