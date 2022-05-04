package org.dcsa.core.extendedrequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.exception.ConcreteRequestErrorMessageException;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.function.BiConsumer;

@RequiredArgsConstructor(staticName = "of")
class QueryFieldRestrictionImpl implements QueryFieldRestriction {

  @Getter
  @NotNull
  private final List<String> defaultValue;
  private final BiConsumer<QueryField, List<String>> validator;

  static final BiConsumer<QueryField, List<String>> INTERNAL_VARIABLE = (q, v) -> {
   throw ConcreteRequestErrorMessageException.invalidQuery(q.getJsonName(),
     "The field " + q.getJsonName() + " cannot be used as a query parameter (internal-only)");
  };

  @Override
  public void validateValues(QueryField queryField, List<String> values) {
    if (validator != null) {
      validator.accept(queryField, values);
    }
  }
}
