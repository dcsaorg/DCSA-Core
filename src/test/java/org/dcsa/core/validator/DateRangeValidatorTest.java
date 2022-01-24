package org.dcsa.core.validator;

import lombok.Data;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@ExtendWith(MockitoExtension.class)
public class DateRangeValidatorTest {

    private final LocalDate DATE_19900101 = LocalDate.of(1990, 1, 1);
    private final LocalDate DATE_20200101 = LocalDate.of(2020, 1, 1);

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Test
    public void testValidatorDefaults() {
        // Valid cases
        Assertions.assertTrue(validate(DateRangeDefaults.of(DATE_19900101, DATE_20200101)));
        Assertions.assertTrue(validate(DateRangeDefaults.of(DATE_19900101, DATE_19900101)));
        Assertions.assertTrue(validate(DateRangeDefaults.of(null, DATE_20200101)));
        Assertions.assertTrue(validate(DateRangeDefaults.of(DATE_19900101, null)));
        Assertions.assertTrue(validate(DateRangeDefaults.of(null, null)));

        // Invalid
        Assertions.assertFalse(validate(DateRangeDefaults.of(DATE_20200101, DATE_19900101)));
    }

    @Test
    public void testValidatorAtLeastOne() {
        // Valid cases
        Assertions.assertTrue(validate(DateRangeAtLeastOne.of(DATE_19900101, DATE_20200101)));
        Assertions.assertTrue(validate(DateRangeAtLeastOne.of(DATE_19900101, DATE_19900101)));
        Assertions.assertTrue(validate(DateRangeAtLeastOne.of(null, DATE_20200101)));
        Assertions.assertTrue(validate(DateRangeAtLeastOne.of(DATE_19900101, null)));

        // Invalid
        Assertions.assertFalse(validate(DateRangeAtLeastOne.of(DATE_20200101, DATE_19900101)));
        Assertions.assertFalse(validate(DateRangeAtLeastOne.of(null, null)));
    }


    @Test
    public void testValidatorNotEqual() {
        // Valid cases
        Assertions.assertTrue(validate(DateRangeNotEqual.of(DATE_19900101, DATE_20200101)));
        Assertions.assertTrue(validate(DateRangeNotEqual.of(null, DATE_20200101)));
        Assertions.assertTrue(validate(DateRangeNotEqual.of(DATE_19900101, null)));
        Assertions.assertTrue(validate(DateRangeNotEqual.of(null, null)));

        // Invalid
        Assertions.assertFalse(validate(DateRangeNotEqual.of(DATE_20200101, DATE_19900101)));
        Assertions.assertFalse(validate(DateRangeNotEqual.of(DATE_19900101, DATE_19900101)));
    }

    @Test
    public void testInvalidAnnotation() {
        assertInvalidAnnotation(MissingEndDate.of(DATE_19900101));
        assertInvalidAnnotation(ConflictingFieldTypes.of(DATE_19900101, null));
        assertInvalidAnnotation(ConflictingMethodTypes.of(DATE_19900101, DATE_20200101));
        assertInvalidAnnotation(ConflictingFieldVsMethodTypes.of(DATE_19900101, DATE_20200101));
        // NotDeclaredAsTemporal should fail even though the values are dates because we want to
        // emulate a compiler level type check with the exception.
        assertInvalidAnnotation(NotDeclaredAsTemporal.of(DATE_19900101, DATE_20200101));
        assertInvalidAnnotation(GetterThrowsException.of(DATE_19900101, DATE_20200101));
    }

    @Test
    public void testNull() {
        // Not entirely sure this case makes sense, but it is a conditional in the validator
        Assertions.assertTrue(validate(null));
    }

    private boolean validate(Object value) {
        DateRangeValidator rangeValidator = new DateRangeValidator();
        if (value != null) {
            DateRange rangeAnno = value.getClass().getAnnotation(DateRange.class);
            rangeValidator.initialize(rangeAnno);
        }
        return rangeValidator.isValid(value, constraintValidatorContext);
    }

    private void assertInvalidAnnotation(Object value) {
        Assertions.assertThrows(IllegalStateException.class, () -> this.validate(value));
    }

    @Data(staticConstructor = "of")
    @DateRange(startField = "startDate", endField = "endDate")
    private static class DateRangeDefaults {
        private final LocalDate startDate;
        private final LocalDate endDate;
    }

    @Data(staticConstructor = "of")
    @DateRange(startField = "startDate", endField = "endDate", nullHandling = DateRange.NullHandling.AT_LEAST_ONE_MUST_BE_NONNULL)
    private static class DateRangeAtLeastOne {
        private final LocalDate startDate;
        private final LocalDate endDate;
    }

    @Data(staticConstructor = "of")
    @DateRange(startField = "startDate", endField = "endDate", equalHandling = DateRange.EqualHandling.EQUAL_INVALID)
    private static class DateRangeNotEqual {
        private final LocalDate startDate;
        private final LocalDate endDate;
    }

    @Data(staticConstructor = "of")
    @DateRange(startField = "startDate", endField = "endDate", equalHandling = DateRange.EqualHandling.EQUAL_INVALID)
    private static class MissingEndDate {
        private final LocalDate startDate;
    }

    @Data(staticConstructor = "of")
    @DateRange(startField = "startDate", endField = "endDate", equalHandling = DateRange.EqualHandling.EQUAL_INVALID)
    private static class ConflictingFieldTypes {
        private final LocalDate startDate;
        private final ZonedDateTime endDate;
    }

    @Data(staticConstructor = "of")
    @DateRange(startField = "startDate", endField = "endDate", equalHandling = DateRange.EqualHandling.EQUAL_INVALID)
    private static class ConflictingMethodTypes {
        private final LocalDate startDate;
        private final LocalDate endDate;

        // start date uses LocalDate via @Data
        @SuppressWarnings("unused")
        public ZonedDateTime getEndDate() {
            return null;
        }
    }

    @Data(staticConstructor = "of")
    @DateRange(startField = "startDate", endField = "endDate", equalHandling = DateRange.EqualHandling.EQUAL_INVALID)
    private static class ConflictingFieldVsMethodTypes {
        private final LocalDate startDate;
        private final LocalDate endDate;

        @SuppressWarnings("unused")
        public ZonedDateTime getStartDate() {
            return null;
        }

        @SuppressWarnings("unused")
        public ZonedDateTime getEndDate() {
            return null;
        }
    }

    @Data(staticConstructor = "of")
    @DateRange(startField = "startDate", endField = "endDate", equalHandling = DateRange.EqualHandling.EQUAL_INVALID)
    private static class NotDeclaredAsTemporal {
        private final Object startDate;
        private final Object endDate;
    }

    @Data(staticConstructor = "of")
    @DateRange(startField = "startDate", endField = "endDate")
    private static class GetterThrowsException {
        private final LocalDate startDate;
        private final LocalDate endDate;

        @SuppressWarnings("unused")
        public LocalDate getEndDate() {
            throw new RuntimeException("Boom!");
        }
    }
}
