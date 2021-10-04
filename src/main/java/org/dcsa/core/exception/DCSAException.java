package org.dcsa.core.exception;

/**
 * This class is intended to be used as a super class for dcsa created custom exceptions.
 * We then use this to handle custom exceptions in GlobalExceptionHandler. *
 */
public class DCSAException extends RuntimeException {

  public DCSAException(String errorMsg) {
    super(errorMsg);
  }

  public DCSAException(String errorMsg, Throwable throwable) {
    super(errorMsg, throwable);
  }
}
