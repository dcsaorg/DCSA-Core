package org.dcsa.core.validator;

import lombok.Data;
import lombok.SneakyThrows;
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

    private boolean validate(GetIMO container) {
        VesselIMONumberValidator imoValidator = new VesselIMONumberValidator();
        String value = null;
        if (container != null) {
            ValidVesselIMONumber imoAnno = container.getClass().getAnnotation(ValidVesselIMONumber.class);
            imoValidator.initialize(imoAnno);
            value = container.getVesselIMONumber();
        }
        return imoValidator.isValid(value, constraintValidatorContext);
    }

    private interface GetIMO {
        String getVesselIMONumber();
    }

    // Put the annotation on the container (it will apply to everything, and it is easier to get the annotation)
    @ValidVesselIMONumber
    @Data(staticConstructor = "of")
    private static class TestEntity implements GetIMO {
        private final String vesselIMONumber;
    }

    @ValidVesselIMONumber(allowNull = true)
    @Data(staticConstructor = "of")
    private static class TestEntityAllowingNull implements GetIMO {
        private final String vesselIMONumber;
    }
}
