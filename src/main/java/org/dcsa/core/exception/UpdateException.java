package org.dcsa.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Deprecated
/**
 * @deprecated see {@link org.dcsa.core.exception.ConcreteRequestErrorMessageException}.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class UpdateException extends DCSAException {
  public UpdateException(String errorMessage) {
    super(errorMessage);
  }
}
