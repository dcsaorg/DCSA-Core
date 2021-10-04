package org.dcsa.core.exception.handler;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.core.exception.*;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DCSAException.class)
  public void handleDCSAExceptions(DCSAException dcsaEx) {
    log.error(
        "{} ({}) - {}",
        this.getClass().getSimpleName(),
        dcsaEx.getClass().getSimpleName(),
        dcsaEx.getMessage());
    logExceptionTraceIfEnabled(dcsaEx);
    throw dcsaEx;
  }

  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid input.")
  @ExceptionHandler(ConstraintViolationException.class)
  public void badRequest(ConstraintViolationException cvex) {
    log.error("Input error : {}", cvex.getConstraintViolations());
    logExceptionTraceIfEnabled(cvex);
  }

  @ExceptionHandler(BadSqlGrammarException.class)
  public void handle(BadSqlGrammarException ex) {
    if ("22001".equals(ex.getR2dbcException().getSqlState())) {
      // The error with code 22001 is thrown when trying to insert a value that is too long for the
      // column
      if (ex.getSql().startsWith("INSERT INTO")) {
        log.warn(
            this.getClass().getSimpleName()
                + " insert into error! - "
                + ex.getR2dbcException().getMessage());
        throw new CreateException("Trying to insert a string value that is too long");
      } else {
        log.warn(
            this.getClass().getSimpleName()
                + " update error! - "
                + ex.getR2dbcException().getMessage());
        throw new UpdateException("Trying to update a string value that is too long");
      }
    } else if ("42804".equals(ex.getR2dbcException().getSqlState())) {
      log.error(
          this.getClass().getSimpleName()
              + " database error! - "
              + ex.getR2dbcException().getMessage());
      throw new DatabaseException(
          "Internal mismatch between backEnd and database - please see log");
    } else {
      log.error(this.getClass().getSimpleName() + " R2dbcException!", ex);
      throw new DatabaseException("Internal error with database operation - please see log");
    }
  }

  // Spring's default handler for unknown JSON properties is useless for telling the user
  // what they did wrong.  Unwrap the inner UnrecognizedPropertyException, which has a
  // considerably better message.
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  @ExceptionHandler(DecodingException.class)
  public void handleJsonDecodeException(DecodingException ce) {
    if (ce.getCause() instanceof UnrecognizedPropertyException) {
      throw new InputParsingException(ce.getCause().getLocalizedMessage(), ce);
    }
    throw new InputParsingException(ce.getLocalizedMessage(), ce);
  }

  @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
  @ExceptionHandler(Exception.class)
  public void handleAllExceptions(Exception ex) {
    log.error("{} error!", this.getClass().getSimpleName(), ex);
    logExceptionTraceIfEnabled(ex);
  }

  private void logExceptionTraceIfEnabled(Exception ex) {
    if (log.isTraceEnabled()) {
      log.trace("Verbose error : ", ex);
    }
  }
}
