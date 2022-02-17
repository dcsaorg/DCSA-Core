package org.dcsa.core.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Ensures an attribute is a member of a given enum.
 *
 * Normally, type checking would enforce this constraint.  This enum is useful
 * where type checking cannot be applied (e.g., with query parameters).
 *
 * Example usage:
 *
 * <pre>{@code
 * @RestController
 * @Validated
 * public class FooController {
 *   @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE})
 *   public Flux<Foo> findAll(
 *        @RequestParam(value = "fooStatusTypeCode", required = false)
 *        @ValidEnum(clazz = FooStatusTypeCode.class)
 *        String fooStatusTypeCodeAsString
 *       ) {
 *       // This is guaranteed to work by @ValidEnum
 *       FooStatusTypeCode statusTypeCode = FooStatusTypeCode.valueOf(fooStatusTypeCodeAsString);
 *       // ... additional content here
 *   }
 * }
 *
 * public enum FooStatusTypeCode {
 *     CREATED,
 *     STARTED,
 *     PENDING_FEEDBACK,
 *     FINISHED;
 * }
 *
 * }</pre>
 */
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = EnumValidator.class)
public @interface ValidEnum {
  Class<? extends Enum> clazz();

  String message() default "must be a valid {clazz}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
