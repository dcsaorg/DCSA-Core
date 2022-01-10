package org.dcsa.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class NotFoundException extends ConcreteRequestErrorMessageException {

  /**
   * @deprecated use {@link org.dcsa.core.exception.ConcreteRequestErrorMessageException#notFound(String)}.
   */
  @Deprecated
  public NotFoundException(String errorMessage) {
    super(null, null, errorMessage, null);
  }

  NotFoundException(String reason, Object reference, String message, Throwable cause) {
    super(reason, reference, message, cause);
  }
}
