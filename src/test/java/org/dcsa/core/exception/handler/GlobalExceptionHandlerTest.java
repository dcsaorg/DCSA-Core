package org.dcsa.core.exception.handler;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.spi.R2dbcException;
import lombok.Data;
import lombok.SneakyThrows;
import org.dcsa.core.exception.ConcreteRequestErrorMessageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.HashSet;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;

@DisplayName("Tests for GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  WebTestClient webTestClient;
  Exception exception;

  @RestController()
  @Validated
  class TestController {

    @SneakyThrows
    @GetMapping("/test")
    public Mono<String> test() {
      throw exception;
    }
  }

  @BeforeEach
  void init() {

    webTestClient =
        WebTestClient.bindToController(new TestController())
            .controllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @DisplayName("Tests for ConcreteRequestErrorMessageException")
  @Nested
  class ConcreteRequestErrorMessageExceptionTest {

    @Test
    @DisplayName("Test for not found exception")
    void testNotFound() {
      exception = ConcreteRequestErrorMessageException.notFound("You can't find me.");
      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isNotFound()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("notFound")
          .jsonPath("$.errors[0].message")
          .isEqualTo("You can't find me.");
    }

    @Test
    @DisplayName("Test for invalid input exception")
    void testInvalidInput() {
      exception =
          ConcreteRequestErrorMessageException.invalidInput(
              "Invalid input", new IllegalArgumentException());
      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput")
          .jsonPath("$.errors[0].message")
          .isEqualTo("Invalid input");
    }

    @Test
    @DisplayName("Test for internal server error")
    void testInternalServerError() {
      exception = ConcreteRequestErrorMessageException.internalServerError("Internal server error");
      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .is5xxServerError()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("internalError")
          .jsonPath("$.errors[0].message")
          .isEqualTo("Internal server error");
    }
  }

  @DisplayName("Tests for constraint violation exceptions")
  @Nested
  class ConstraintViolationExceptionTest {

    @Test
    @DisplayName("Test constraint violation exception with empty ConstraintViolations")
    void testConstraintViolationException() {

      exception = new ConstraintViolationException(new HashSet<>());
      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput");
    }

    @Test
    @DisplayName("Test constraint violation with invalid input")
    void testConstraintViolationExceptionInputParameter() {

      ValidationObject testObject = new ValidationObject();
      testObject.setLimitedField("123456");

      ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
      Validator validator = factory.getValidator();

      exception = new ConstraintViolationException(validator.validate(testObject));

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput")
          .jsonPath(
              "$.errors[0].message",
              anyOf(
                  equalTo(
                      "limitedField size must be between 0 and 5;requiredField must not be null"),
                  equalTo(
                      "requiredField must not be null;limitedField size must be between 0 and 5")));
    }

    @Test
    @DisplayName("Test constraint violation as cause of ServerWebInputException")
    void testServerWebInputException() {

      ValidationObject testObject = new ValidationObject();
      testObject.setLimitedField("123456");

      ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
      Validator validator = factory.getValidator();

      exception =
          new ServerWebInputException(
              "", null, new ConstraintViolationException(validator.validate(testObject)));

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput")
          .jsonPath(
              "$.errors[0].message",
              anyOf(
                  equalTo(
                      "limitedField size must be between 0 and 5;requiredField must not be null"),
                  equalTo(
                      "requiredField must not be null;limitedField size must be between 0 and 5")));
    }

    @Data
    class ValidationObject {
      @NotNull private String requiredField;

      @Size(max = 5)
      private String limitedField;
    }
  }

  @DisplayName("Tests for BadSqlGrammarException")
  @Nested
  class BadSqlGrammarExceptionTest {

    @Test
    @DisplayName("Test BadSqlGrammarException r2dbc 22001 Insert SQL State exception")
    void testSqlStateException() {
      exception =
          new BadSqlGrammarException(
              "test task", "INSERT INTO ", new R2dbcTestException("reason", "22001", 1, null));

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidParameter")
          .jsonPath("$.errors[0].message")
          .isEqualTo("Trying to insert a string value that is too long");
    }

    @Test
    @DisplayName("Test BadSqlGrammarException r2dbc 22001 Update SQL State exception")
    void testSqlStateExceptionUpdate() {
      exception =
          new BadSqlGrammarException(
              "test task", "test statement", new R2dbcTestException("reason", "22001", 1, null));

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidParameter")
          .jsonPath("$.errors[0].message")
          .isEqualTo("Trying to update a string value that is too long");
    }

    @Test
    @DisplayName("Test BadSqlGrammarException r2dbc 42804 SQL State exception")
    void testSqlStateExceptionMismatch() {
      exception =
          new BadSqlGrammarException(
              "test task", "test statement", new R2dbcTestException("reason", "42804", 1, null));

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .is5xxServerError()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("internalError")
          .jsonPath("$.errors[0].message")
          .isEqualTo("Internal mismatch between backEnd and database - please see log");
    }

    @Test
    @DisplayName("Test BadSqlGrammarException")
    void testSqlException() {
      exception =
          new BadSqlGrammarException(
              "test task", "test statement", new R2dbcTestException("reason", "12345", 1, null));

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .is5xxServerError()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("internalError")
          .jsonPath("$.errors[0].message")
          .isEqualTo("Internal error with database operation - please see log");
    }

    class R2dbcTestException extends R2dbcException {

      public R2dbcTestException(String reason, String sqlState, int errorCode, Throwable ex) {
        super(reason, sqlState, errorCode, ex);
      }
    }
  }

  @DisplayName("Tests for ServerWebInputException")
  @Nested
  class ServerWebInputExceptionTest {

    @Test
    @DisplayName("Test for ServerWebInputException invalid UUID")
    void testServerWebInputInvalidUUIDException() {
      exception = new ServerWebInputException("Invalid UUID string:");

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidParameter")
          .jsonPath("$.errors[0].message")
          .isEqualTo("Input was not a valid UUID format");
    }

    @Test
    @DisplayName("Test for ServerWebInputException cause empty DecodingException")
    void testServerWebInputExceptionWithEmptyDecodingException() {
      exception = new ServerWebInputException("DecodingException", null, new DecodingException(""));

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput");
    }

    @Test
    @DisplayName("Test for generic ServerWebInputException")
    void testServerWebInputException() {
      exception = new ServerWebInputException("");

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .is5xxServerError()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("internalError");
    }
  }

  @DisplayName("Tests for DecodingException")
  @Nested
  class DecodingExceptionTest {

    @Test
    @DisplayName("Test for flat DecodingException")
    void testDecodingException() {
      exception = new DecodingException("flat");
      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput");
    }

    @Test
    @DisplayName("Test for JsonMappingException as cause of DecodingException")
    void testDecodingExceptionCausedByJsonMappingException() {
      exception =
          new DecodingException(
              "JsonMappingException", new JsonMappingException("testMessage", JsonLocation.NA));
      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput")
          .jsonPath("$.errors[0].message")
          .isEqualTo(" The error is associated with the attribute .");
    }

    @Test
    @DisplayName("Test for JsonPropertyException as cause of UnrecognizedPropertyException")
    void testDecodingExceptionCausedByUnrecognizedPropertyException() {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.findAndRegisterModules();

      try {
        objectMapper.readValue("{\"TestPoJo\": {\"test\":\"value\"}}", TestPoJo.class);
      } catch (JsonProcessingException e) {
        exception = new DecodingException("JsonPropertyException", e);
      }

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput")
          .jsonPath("$.errors[0].message")
          .isEqualTo(
              "Unrecognized field \"TestPoJo\" , not marked as ignorable at [Source: \"{\"TestPoJo\": "
                  + "{\"test\":\"value\"}}\"; line: 1, column: 15]  " +
                "The error is associated with the attribute TestPoJo[\"TestPoJo\"].");
    }

    @Test
    @DisplayName("Test for InvalidFormatException with Date as cause of UnrecognizedPropertyException")
    void testDecodingExceptionCausedByInvalidFormatExceptionDate() {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.findAndRegisterModules();

      try {
        objectMapper.readValue(
            "{\"testString\":\"value\", \"testDate\": \"01-12-2022\"}", TestPoJo.class);
      } catch (JsonProcessingException e) {
        exception = new DecodingException("JsonPropertyException", e);
      }

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput")
          .jsonPath("$.errors[0].message")
          .isEqualTo(
              "Invalid format for date field. " +
                "The value \"01-12-2022\" cannot be parsed via the pattern \"YYYY-MM-DD\". " +
                "Please check the input. The error is associated with the attribute TestPoJo[\"testDate\"].");
    }

    @Test
    @DisplayName("Test for InvalidFormatException with DateTime as cause of UnrecognizedPropertyException")
    void testDecodingExceptionCausedByInvalidFormatExceptionDateTime() {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.findAndRegisterModules();

      try {
        objectMapper.readValue(
          "{\"testString\":\"value\", \"testDateTime\": \"2022-01-23T35:12:00\"}", TestPoJo.class);
      } catch (JsonProcessingException e) {
        exception = new DecodingException("JsonPropertyException", e);
      }

      webTestClient
          .get()
          .uri("/test")
          .exchange()
          .expectStatus()
          .isBadRequest()
          .expectBody()
          .jsonPath("$.httpMethod")
          .isEqualTo("GET")
          .jsonPath("$.errors[0].reason")
          .isEqualTo("invalidInput")
          .jsonPath("$.errors[0].message")
          .isEqualTo(
              "Invalid format for date time field. The value \"2022-01-23T35:12:00\" " +
                "cannot be parsed via the patterns \"YYYY-MM-DD'T'HH:MM:SS+ZZZZ\" or \"YYYY-MM-DD'T'HH:MM:SS'Z'\". " +
                "Please check the input. The error is associated with the attribute TestPoJo[\"testDateTime\"].");
    }
  }

  @DisplayName("Tests for DataIntegrityViolationException")
  @Nested
  class DataIntegrityViolationExceptionTest {

    @Test
    @DisplayName("DataIntegrityViolationException")
    void testDataIntegrityViolationException() {
      exception = new DataIntegrityViolationException("Data needs to have integrity");

      webTestClient
        .get()
        .uri("/test")
        .exchange()
        .expectStatus()
        .is4xxClientError()
        .expectBody()
        .jsonPath("$.httpMethod")
        .isEqualTo("GET")
        .jsonPath("$.errors[0].reason")
        .isEqualTo("conflict")
        .jsonPath("$.errors[0].message")
        .isEqualTo("Data needs to have integrity");
    }
  }

  @DisplayName("Tests for Generic Exception")
  @Nested
  class GenericExceptionTest {

    @Test
    @DisplayName("Test for a generic exception")
    void testGenericException() {
      exception = new Exception("Generic exception");

      webTestClient
        .get()
        .uri("/test")
        .exchange()
        .expectStatus()
        .is5xxServerError();
    }

    @Test
    @DisplayName("Test for a ResponseStatusException")
    void testResponseStatusException() {
      exception = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);

      webTestClient
        .get()
        .uri("/test")
        .exchange()
        .expectStatus()
        .is5xxServerError()
        .expectBody()
        .jsonPath("$.httpMethod")
        .isEqualTo("GET")
        .jsonPath("$.errors[0].reason")
        .isEqualTo("internalError")
        .jsonPath("$.errors[0].message")
        .isEqualTo("500 INTERNAL_SERVER_ERROR");
    }
  }
}
