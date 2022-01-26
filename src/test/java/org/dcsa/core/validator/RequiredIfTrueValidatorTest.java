package org.dcsa.core.validator;

import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
public class RequiredIfTrueValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Test
    public void testValidatorDefaults() {
        // Valid cases
        Assertions.assertTrue(validate(RequiredIfTrueDefaults.of(false, LocalDate.of(1990, 1, 1))));
        Assertions.assertTrue(validate(RequiredIfTrueDefaults.of(false, "DATE_19900101")));
        Assertions.assertTrue(validate(RequiredIfTrueDefaults.of(false, true)));
        Assertions.assertTrue(validate(RequiredIfTrueDefaults.of(false, new boolean[] { true, false })));
        Assertions.assertTrue(validate(RequiredIfTrueDefaults.of(false, null)));

        // Invalid
        Assertions.assertFalse(validate(RequiredIfTrueDefaults.of(true, null)));
    }

    @Test
    public void testValidatorPrimitiveDefaults() {
        // Valid cases
        Assertions.assertTrue(validate(RequiredIfTruePrimitiveDefaults.of(false, LocalDate.of(1990, 1, 1))));
        Assertions.assertTrue(validate(RequiredIfTruePrimitiveDefaults.of(false, "DATE_19900101")));
        Assertions.assertTrue(validate(RequiredIfTruePrimitiveDefaults.of(false, true)));
        Assertions.assertTrue(validate(RequiredIfTruePrimitiveDefaults.of(false, new boolean[] { true, false })));
        Assertions.assertTrue(validate(RequiredIfTruePrimitiveDefaults.of(false, null)));

        // Invalid
        Assertions.assertFalse(validate(RequiredIfTruePrimitiveDefaults.of(true, null)));
    }

    @Test
    public void testInvalidAnnotation() {
        // Probably unnecessary
        assertInvalidAnnotation(RequiredIfTrueInvalid.of("I'm a string!", null));
    }

    @Test
    public void testNull() {
        // Not entirely sure this case makes sense, but it is a conditional in the validator
        Assertions.assertFalse(validate(null));
    }

    private boolean validate(Object value) {
        RequiredIfTrueValidator rangeValidator = new RequiredIfTrueValidator();
        if (value != null) {
            RequiredIfTrue rangeAnno = value.getClass().getAnnotation(RequiredIfTrue.class);
            rangeValidator.initialize(rangeAnno);
        }
        return rangeValidator.isValid(value, constraintValidatorContext);
    }

    private void assertInvalidAnnotation(Object value) {
        Assertions.assertThrows(IllegalStateException.class, () -> this.validate(value));
    }

    @Data(staticConstructor = "of")
    @RequiredIfTrue(isFieldReferenceRequired = "isRequired", fieldReference = "requirement")
    private static class RequiredIfTrueDefaults {
        private final Boolean isRequired;
        private final Object requirement;
    }

    @Data(staticConstructor = "of")
    @RequiredIfTrue(isFieldReferenceRequired = "isRequired", fieldReference = "requirement")
    private static class RequiredIfTruePrimitiveDefaults {
        private final boolean isRequired;
        private final Object requirement;
    }

    @Data(staticConstructor = "of")
    @RequiredIfTrue(isFieldReferenceRequired = "isRequired", fieldReference = "requirement")
    private static class RequiredIfTrueInvalid {
        private final Object isRequired;
        private final Object requirement;
    }
}
