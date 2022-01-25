package org.dcsa.core.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = RequiredIfTrueValidator.class)
public @interface RequiredIfTrue {

    String isFieldReferenceRequired();

    String fieldReference();

    boolean allowNull() default false;

    String message() default "if {isFieldReferenceRequired} is true then {fieldReference} cannot be null";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    @interface List {
        RequiredIfTrue[] value();
    }
}
