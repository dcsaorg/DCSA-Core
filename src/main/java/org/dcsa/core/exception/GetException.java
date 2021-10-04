package org.dcsa.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class GetException extends DCSAException {
  public GetException(String errorMessage) {
    super(errorMessage);
  }
}
