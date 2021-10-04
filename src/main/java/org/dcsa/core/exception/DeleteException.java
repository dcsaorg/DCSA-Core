package org.dcsa.core.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DeleteException extends DCSAException {
  public DeleteException(String errorMessage) {
    super(errorMessage);
  }
}
