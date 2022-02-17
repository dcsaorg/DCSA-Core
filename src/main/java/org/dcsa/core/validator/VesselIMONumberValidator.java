package org.dcsa.core.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public final class VesselIMONumberValidator implements ConstraintValidator<ValidVesselIMONumber, String> {

  private boolean allowNull = false;

  @Override
  public void initialize(ValidVesselIMONumber constraintAnnotation) {
    allowNull = constraintAnnotation.allowNull();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {

    if (null == value) {
      return allowNull;
    }

    if (value.length() != 7) {
      return false;
    }

    return validateIMOCheckSum(value);
  }

  private boolean validateIMOCheckSum(String vesselIMONumber) {
    int sum = 0;
    assert vesselIMONumber.length() == 7;
    for (int i = 0; i < 6; i++) {
      char c = vesselIMONumber.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
      sum += (7 - i) * Character.getNumericValue(c);
    }
    String s = String.valueOf(sum);
    return vesselIMONumber.charAt(vesselIMONumber.length() - 1) != s.charAt(s.length() - 1);
  }
}
