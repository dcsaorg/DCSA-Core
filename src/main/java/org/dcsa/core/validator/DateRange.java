package org.dcsa.core.validator;

import javax.validation.Constraint;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({TYPE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
public @interface DateRange {

    /**
     * Name of the field that defines the start of the range
     */
    String startField();

    /**
     * Name of the field that defines the end of the range
     */
    String endField();

    /**
     * Whether at least one of the fields must be provided.
     *
     * This is only useful if both are nullable, but you want to ensure that at
     * least one of them is present.
     *
     * If you want to require one or both of the fields to be not null, then please
     * use @{@link javax.validation.constraints.NotNull} on the relevant fields in
     * addition to this annotation.
     */
    NullHandling nullHandling() default NullHandling.BOTH_CAN_BE_NULL;

    /**
     * Whether the startField and endField can have the same value <i>>when not null</i>
     *
     * This does not affect null handling. Use {@link #nullHandling()} for deciding whether
     * both fields may be null.
     */
    EqualHandling equalHandling() default EqualHandling.EQUAL_OK;

    enum EqualHandling {
        EQUAL_OK,
        EQUAL_INVALID,
    }

    /**
     * How to handle when one of both of the fields are null
     *
     * For "neither may be null", please use @NonNull instead.
     */
    enum NullHandling {
        AT_LEAST_ONE_MUST_BE_NONNULL,
        BOTH_CAN_BE_NULL,
    }
}
