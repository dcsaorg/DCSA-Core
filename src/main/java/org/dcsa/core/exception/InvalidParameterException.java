package org.dcsa.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Deprecated
/**
 * @deprecated use {@link org.dcsa.core.exception.ConcreteRequestErrorMessageException#invalidParameter(String)}.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidParameterException extends DCSAException {
  public InvalidParameterException(String errorMessage) {
    super(errorMessage);
  }
}
