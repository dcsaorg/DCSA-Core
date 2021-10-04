package org.dcsa.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InputParsingException extends DCSAException {
  public InputParsingException(String message, Throwable cause) {
    super(message, cause);
  }
}
