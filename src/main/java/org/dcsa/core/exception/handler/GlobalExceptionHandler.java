package org.dcsa.core.exception.handler;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.core.exception.ConcreteRequestErrorMessageException;
import org.dcsa.core.exception.DCSAException;
import org.dcsa.core.model.transferobjects.ConcreteRequestErrorMessageTO;
import org.dcsa.core.model.transferobjects.RequestFailureTO;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebInputException;

import javax.validation.ConstraintViolationException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ConcreteRequestErrorMessageException.class)
  public ResponseEntity<RequestFailureTO> handleConcreteRequestErrorMessageException(
    ServerHttpRequest serverHttpRequest, ConcreteRequestErrorMessageException ex) {
    ResponseStatus responseStatusAnnotation = ex.getClass().getAnnotation(ResponseStatus.class);
    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    ConcreteRequestErrorMessageTO errorEntity = ex.asConcreteRequestMessage();
    if (responseStatusAnnotation != null) {
      httpStatus = responseStatusAnnotation.value();
    }

    RequestFailureTO failureTO =
      new RequestFailureTO(
        serverHttpRequest.getMethodValue(),
        serverHttpRequest.getURI().toString(),
        List.of(errorEntity),
        httpStatus);
    return new ResponseEntity<>(failureTO, httpStatus);
  }

  @Deprecated
  @ExceptionHandler(DCSAException.class)
  public ResponseEntity<RequestFailureTO> handleDCSAExceptions(
    ServerHttpRequest serverHttpRequest, DCSAException dcsaEx) {

    log.debug(
      "{} ({}) - {}",
      this.getClass().getSimpleName(),
      dcsaEx.getClass().getSimpleName(),
      dcsaEx.getMessage());
    logExceptionTraceIfEnabled(dcsaEx);

    ResponseStatus responseStatusAnnotation = dcsaEx.getClass().getAnnotation(ResponseStatus.class);

    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

    if (responseStatusAnnotation != null) {
      httpStatus = responseStatusAnnotation.value();
    }

    ConcreteRequestErrorMessageTO errorEntity =
      new ConcreteRequestErrorMessageTO(httpStatus.getReasonPhrase(), dcsaEx.getMessage());

    RequestFailureTO failureTO =
      new RequestFailureTO(
        serverHttpRequest.getMethodValue(),
        serverHttpRequest.getURI().toString(),
        List.of(errorEntity),
        httpStatus);
    return new ResponseEntity<>(failureTO, httpStatus);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<RequestFailureTO> badRequest(
    ServerHttpRequest serverHttpRequest, ConstraintViolationException cvex) {
    String exceptionMessage = null;
    if (Objects.nonNull(cvex.getConstraintViolations())) {
      log.debug("Input error : {}", cvex.getConstraintViolations());
      exceptionMessage =
        cvex.getConstraintViolations().stream()
          .filter(Objects::nonNull)
          .map(
            constraintViolation ->
              constraintViolation.getPropertyPath()
                + " "
                + constraintViolation.getMessage())
          .collect(Collectors.joining(";"));
    }
    logExceptionTraceIfEnabled(cvex);

    return handleConcreteRequestErrorMessageException(
      serverHttpRequest,
      ConcreteRequestErrorMessageException.invalidInput(exceptionMessage, cvex));
  }

  @ExceptionHandler(BadSqlGrammarException.class)
  public ResponseEntity<RequestFailureTO> handle(
    ServerHttpRequest serverHttpRequest, BadSqlGrammarException ex) {
    if ("22001".equals(ex.getR2dbcException().getSqlState())) {
      // The error with code 22001 is thrown when trying to insert a value that is too long for the
      // column
      if (ex.getSql().startsWith("INSERT INTO")) {
        log.debug(
          "{} insert into error! - {}",
          this.getClass().getSimpleName(),
          ex.getR2dbcException().getMessage());
        return handleConcreteRequestErrorMessageException(
          serverHttpRequest,
          ConcreteRequestErrorMessageException.invalidParameter(
            "Trying to insert a string value that is too long"));
      } else {
        log.debug(
          "{} update error! - {}",
          this.getClass().getSimpleName(),
          ex.getR2dbcException().getMessage());
        return handleConcreteRequestErrorMessageException(
          serverHttpRequest,
          ConcreteRequestErrorMessageException.invalidParameter(
            "Trying to update a string value that is too long"));
      }
    } else if ("42804".equals(ex.getR2dbcException().getSqlState())) {
      return handleConcreteRequestErrorMessageException(
        serverHttpRequest,
        ConcreteRequestErrorMessageException.internalServerError(
          "Internal mismatch between backEnd and database - please see log", ex));
    } else {
      return handleConcreteRequestErrorMessageException(
        serverHttpRequest,
        ConcreteRequestErrorMessageException.internalServerError(
          "Internal error with database operation - please see log", ex));
    }
  }

  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<RequestFailureTO> handle(
    ServerHttpRequest serverHttpRequest, ServerWebInputException ex) {
    if (ex.getMessage() != null && ex.getMessage().contains("Invalid UUID string:")) {
      return handleConcreteRequestErrorMessageException(
        serverHttpRequest,
        ConcreteRequestErrorMessageException.invalidParameter(
          "Input was not a valid UUID format", ex));
    } else if (ex.getCause() instanceof DecodingException) {
      return handleDecodingExceptionImpl(serverHttpRequest, (DecodingException) ex.getCause());
    } else if (ex.getCause() instanceof ConstraintViolationException) {
      return badRequest(serverHttpRequest, (ConstraintViolationException) ex.getCause());
    } else {
//      throw ex;
      return handleConcreteRequestErrorMessageException(
        serverHttpRequest,
        ConcreteRequestErrorMessageException.invalidInput(ex.getMessage(), ex));
    }
  }

  // Spring's default handler for unknown JSON properties is useless for telling the user
  // what they did wrong.  Unwrap the inner UnrecognizedPropertyException, which has a
  // considerably better message.
  @ExceptionHandler(DecodingException.class)
  public ResponseEntity<RequestFailureTO> handleJsonDecodeException(
    ServerHttpRequest serverHttpRequest, DecodingException ce) {
    return handleDecodingExceptionImpl(serverHttpRequest, ce);
  }

  private ResponseEntity<RequestFailureTO> handleDecodingExceptionImpl(
    ServerHttpRequest serverHttpRequest, DecodingException ce) {
    String attributeReference = "";
    if (ce.getCause() instanceof JsonMappingException) {
      attributeReference = ((JsonMappingException) ce.getCause()).getPathReference();
      // Remove leading java package name (Timestamp["foo"] looks better than
      // org.dcsa.jit.model.Timestamp["foo"])
      attributeReference = attributeReference.replaceFirst("^([a-zA-Z0-9]+[.])*+", "");
      attributeReference =
        " The error is associated with the attribute " + attributeReference + ".";
    }
    if (ce.getCause() instanceof UnrecognizedPropertyException) {
      // Unwrap one layer of exception message (the outer message is "useless")
      attributeReference =
        ce.getCause().getLocalizedMessage().replaceAll("\\s*\\([^\\)]*\\)\\s*", " ")
          + attributeReference;
    }

    if (ce.getCause() instanceof InvalidFormatException) {
      InvalidFormatException ife = (InvalidFormatException) ce.getCause();
      // Per type messages where it makes sense to provide custom messages
      if (OffsetDateTime.class.isAssignableFrom(ife.getTargetType())) {
        attributeReference =
          "Invalid format for date time field. The value \""
            + ife.getValue()
            + "\" cannot be parsed via the patterns \"YYYY-MM-DD'T'HH:MM:SS+ZZZZ\" "
            + "or \"YYYY-MM-DD'T'HH:MM:SS'Z'\". Please check the input."
            + attributeReference;
      }
      if (LocalDate.class.isAssignableFrom(ife.getTargetType())) {
        attributeReference =
          "Invalid format for date field. The value \""
            + ife.getValue()
            + "\" cannot be parsed via the pattern \"YYYY-MM-DD\". Please check the input."
            + attributeReference;
      }
    }
    return handleConcreteRequestErrorMessageException(
      serverHttpRequest,
      ConcreteRequestErrorMessageException.invalidInput(attributeReference, ce));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> handleDataIntegrityViolationException(
    ServerHttpRequest serverHttpRequest, DataIntegrityViolationException ex) {
    // For when the database catches an inconsistency.  They are not the best of error messages
    // but we should ensure they at least have the proper HTTP code.
    return handleConcreteRequestErrorMessageException(
      serverHttpRequest,
      ConcreteRequestErrorMessageException.conflict(ex.getLocalizedMessage(), ex));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleAllExceptions(ServerHttpRequest serverHttpRequest, Exception ex) {
    log.warn("Unhandled exception", ex);
    return handleConcreteRequestErrorMessageException(
      serverHttpRequest,
      ConcreteRequestErrorMessageException.internalServerError(ex.getMessage(), ex));
  }

  private void logExceptionTraceIfEnabled(Exception ex) {
    if (log.isTraceEnabled()) {
      log.trace("Verbose error : ", ex);
    }
  }
}
