package org.dcsa.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class DatabaseException extends DCSAException {
  public DatabaseException(String errorMessage) {
    super(errorMessage);
  }

  public DatabaseException(String errorMessage, Throwable throwable) {
    super(errorMessage, throwable);
  }
}
