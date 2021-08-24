package org.dcsa.core.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = VesselIMONumberValidator.class)
public @interface ValidVesselIMONumber {

    boolean allowNull() default false;

    String message() default "must be a valid Vessel IMO Number";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
