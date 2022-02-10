package org.dcsa.core.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Enforce that a given field only uses a particular subset of an enum
 *
 * Example usage:
 *
 * <pre>{@code
 * @Data
 * public class FooProgressUpdate {
 *
 *     // In a progress update, the "CREATED" status code is not permitted.
 *     @EnumSubset(anyOf = "STARTED,PENDING_FEEDBACK,FINISHED")
 *     private FooStatusTypeCode fooStatusTypeCode;
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
@Constraint(validatedBy = EnumSubsetValidator.class)
public @interface EnumSubset {
  String[] anyOf();

  String message() default "must be any of {anyOf}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
