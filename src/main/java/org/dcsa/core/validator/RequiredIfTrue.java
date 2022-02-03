package org.dcsa.core.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p><b>Class-level annotation only.</b></p>
 *
 * <p>Validates that if one boolean field is set to true then another field of an arbitrary type is not
 * null.</p>
 *
 * <p>Example of usage:</p>
 *
 * <ul>
 *   <li><p>@RequiredIfTrue(isFieldReferenceRequired = "nameOfBooleanFieldVariable", fieldReference = "nameOfReferenceFieldVariable")</p>
 *       <p>public class className {</p>
 *       <p>  private Boolean nameOfBooleanFieldVariable;</p>
 *       <p>  private String nameOfReferenceFieldVariable;</p>
 *       <p>}</p>
 *   </li>
 * </ul>
 */
@Repeatable(RequiredIfTrue.List.class)
@Target({TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = RequiredIfTrueValidator.class)
public @interface RequiredIfTrue {

  /** Name of the boolean field that determines whether the referenced field is required */
  String isFieldReferenceRequired();

  /** Name of the field referenced by the boolean field */
  String fieldReference();

  String message() default
      "if {isFieldReferenceRequired} is true then {fieldReference} cannot be null";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  @Target({TYPE_USE})
  @Retention(RUNTIME)
  @Documented
  @interface List {
    RequiredIfTrue[] value();
  }
}
