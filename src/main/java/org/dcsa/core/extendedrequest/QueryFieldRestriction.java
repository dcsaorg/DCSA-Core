package org.dcsa.core.extendedrequest;

import org.dcsa.core.exception.ConcreteRequestErrorMessageException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import static org.dcsa.core.extendedrequest.QueryFieldRestrictionImpl.INTERNAL_VARIABLE;

public interface QueryFieldRestriction {

  /**
   * The default value for this query field restriction (if any)
   *
   * @return A list of default values.
   */
  default List<String> defaultValue() {
    return Collections.emptyList();
  }

  /**
   * @return true if this restriction has a default value or not.
   */
  default boolean hasDefault() {
    return !defaultValue().isEmpty();
  }

  /**
   * Implement a validation to apply to a given query field
   *
   * Note that this method is <b>not</b> applied to default values.
   *
   * @param queryField The query field the validation is applied to
   * @param values The values provided by the client.
   */
  void validateValues(QueryField queryField, List<String> values);

  /**
   * Restriction that declares a QueryField as an internal field with a given value
   *
   * This is mostly useful as an implementation detail where you can add a query
   * field to ensure it is set to a particular value.  As an example, it can be
   * used to force "validUntil=NULL".
   *
   * The concrete QueryField cannot be modified by clients. This is currently
   * enforced by triggered a "400 Bad Request" if the client tries to do so
   * (because implementation-wise, it is not currently possible to "undo" the
   * parameter provided by the client).
   *
   * @param value The value to set the query field to.
   * @return A QueryFieldRestriction that forces the related QueryField
   *         is forced to a certain value.
   */
  static QueryFieldRestriction ensureSetTo(List<String> value) {
    return QueryFieldRestrictionImpl.of(value, INTERNAL_VARIABLE);
  }

  /**
   * Restriction that declares a QueryField as an internal field with a given value
   *
   * This is mostly useful as an implementation detail where you can add a query
   * field to ensure it is set to a particular value.  As an example, it can be
   * used to force "validUntil=NULL".
   *
   * The concrete QueryField cannot be modified by clients. Implementation-wise,
   * this is currently enforced by triggered a "400 Bad Request" if the client
   * tries to do so (because implementation-wise, it is not currently possible
   * to "undo" the parameter provided by the client).
   *
   * @param value The value to set the query field to.
   * @return A QueryFieldRestriction that forces the related QueryField
   *         is forced to a certain value.
   */
  static QueryFieldRestriction ensureSetTo(String value) {
    return ensureSetTo(Collections.singletonList(value));
  }

  /**
   * Enforce that a given query field is restricted to a given "enum" subset
   *
   * This also covers a list of known keywords (even if it is not represented as
   * a Java enum).  By default, the QueryField will use all the given enum values
   * (or known keywords) provided to this restriction. However, the client can
   * choose a "smaller" subset via the query parameter.
   *
   * @param commaListOfEnumValues A string containing a comma separated list of
   *                              valid enum values.  We use a comma separated list
   *                              because these lists tend to need to be used in
   *                              {@code @EnumSubset} annotations as well.
   * @return A QueryFieldRestriction that ensures that the field is restricted
   *  to the given subset of enum values.
   */
  static QueryFieldRestriction enumSubset(String commaListOfEnumValues) {
    String[] values = commaListOfEnumValues.split(",");
    Set<String> validValues = Set.of(values);
    BiConsumer<QueryField, List<String>> enumValidator = (q, userProvidedValues) -> {
      for (String rawValue : userProvidedValues) {
        for (String value : rawValue.split(",")) {
          if (!validValues.contains(value)) {
            throw ConcreteRequestErrorMessageException.invalidQuery(q.getJsonName(),
              "Invalid value \"" + value + "\" in query parameter " + q.getJsonName()
                + ". Only the following values are accepted: " + String.join(", ", values)
              );
          }
        }
      }
    };
    return QueryFieldRestrictionImpl.of(List.of(commaListOfEnumValues), enumValidator);
  }

  /**
   * Provide a default (when associated to a QueryField) but no validation
   *
   * @param defaultValue The default value if the client did not provide a value for this query field.
   * @return A QueryFieldRestriction providing a given default.
   */
  static QueryFieldRestriction withDefault(List<String> defaultValue) {
    return withDefaultAndCustomValidator(defaultValue, null);
  }

  /**
   * Provide a validator for a QueryField
   *
   * @param validator An implementation of the validator.  The validator should throw a
   *                  {@link ConcreteRequestErrorMessageException#invalidQuery(String, String)}
   *                  exception (with the first parameter being {@link QueryField#getJsonName()})
   *                  if the value is invalid.
   * @return A QueryFieldRestriction providing a given default.
   */
  static QueryFieldRestriction withCustomValidator(BiConsumer<QueryField, List<String>> validator) {
    return withDefaultAndCustomValidator(Collections.emptyList(), validator);
  }

  /**
   * Provide a validator and a default value for a QueryField
   *
   * @param defaultValue The default value if the client did not provide a value for this query field.
   * @param validator An implementation of the validator.  The validator should throw a
   *                  {@link ConcreteRequestErrorMessageException#invalidQuery(String, String)}
   *                  exception (with the first parameter being {@link QueryField#getJsonName()})
   *                  if the value is invalid.
   *                  Note: The validator is <i>not</i> applied to the default value.
   * @return A QueryFieldRestriction providing a given default.
   */
  static QueryFieldRestriction withDefaultAndCustomValidator(List<String> defaultValue, BiConsumer<QueryField, List<String>> validator) {
    return QueryFieldRestrictionImpl.of(defaultValue, validator);
  }
}
