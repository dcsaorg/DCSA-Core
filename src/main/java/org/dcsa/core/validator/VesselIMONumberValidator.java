package org.dcsa.core.validator;

import org.dcsa.core.util.ValidationUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class VesselIMONumberValidator implements ConstraintValidator<ValidVesselIMONumber, String> {

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

    try {
      ValidationUtils.validateVesselIMONumber(value);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return true;
  }
}
