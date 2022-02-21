package org.dcsa.core.validator;

import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintValidatorContext;

@ExtendWith(MockitoExtension.class)
public class VesselIMONumberValidatorTest {

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Test
    public void testValidInput() {
        // Actual vessels
        Assertions.assertTrue(validate(TestEntity.of("9267649")));
        Assertions.assertTrue(validate(TestEntity.of("9321483")));  // <-- Used in swagger examples

        // A very simple case of numbers that happens to be valid
        Assertions.assertTrue(validate(TestEntity.of("1234567")));

        // Special-case
        Assertions.assertTrue(validate(TestEntityAllowingNull.of(null)));
    }

    @Test
    public void testValidInvalidInput() {
        // Invalid (checksum is off)
        Assertions.assertFalse(validate(TestEntity.of("1234568")));
        Assertions.assertFalse(validate(TestEntity.of("9321485")));

        // Length issues
        Assertions.assertFalse(validate(TestEntity.of("93214852")));
        Assertions.assertFalse(validate(TestEntity.of("932142")));

        // Not all numbers
        Assertions.assertFalse(validate(TestEntity.of("932142A")));
        Assertions.assertFalse(validate(TestEntity.of("93214A2")));
        Assertions.assertFalse(validate(TestEntity.of("Z321432")));
        Assertions.assertFalse(validate(TestEntity.of("3321Z32")));

        // Null is not allowed by default
        Assertions.assertFalse(validate(TestEntity.of(null)));
    }

    private boolean validate(Object value) {
        DateRangeValidator rangeValidator = new DateRangeValidator();
        if (value != null) {
            DateRange rangeAnno = value.getClass().getAnnotation(DateRange.class);
            rangeValidator.initialize(rangeAnno);
        }
        return rangeValidator.isValid(value, constraintValidatorContext);
    }

    @Data(staticConstructor = "of")
    private static class TestEntity {
        @ValidVesselIMONumber
        private final String vesselIMONumber;
    }
    @Data(staticConstructor = "of")
    private static class TestEntityAllowingNull {
        @ValidVesselIMONumber(allowNull = true)
        private final String vesselIMONumber;
    }
}
