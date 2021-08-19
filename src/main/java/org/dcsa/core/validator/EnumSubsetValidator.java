package org.dcsa.core.validator;

import org.apache.commons.lang3.NotImplementedException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Validator for Enumeration subsets. Works both for queryParameters (where the value to validate is
 * a string) and for Enum defined fields (where the value to validate is an Enum)
 *
 * <p>If the value is a string - then this class supports that the string can be comma separated in
 * order to validate a set of values
 */
public class EnumSubsetValidator implements ConstraintValidator<EnumSubset, Object> {

  private String[] subsetTypes;

  @Override
  public void initialize(EnumSubset constraintAnnotation) {
    this.subsetTypes = constraintAnnotation.anyOf();
  }

  @Override
  public boolean isValid(Object types, ConstraintValidatorContext constraintValidatorContext) {
    if (null == types) {
      return true;
    }

    if (types instanceof String) {
      Stream<String> enumStream =
          ((String) types).contains(",")
              ? Arrays.stream(((String) types).split(","))
              : Stream.of((String) types);
      return enumStream.allMatch(e -> Arrays.asList(subsetTypes).contains(e));
    } else if (types instanceof Enum) {
      return Arrays.asList(subsetTypes).contains(((Enum) types).name());
    } else if (types instanceof List) {
      return ((List<? extends Enum>) types)
          .stream().map(Enum::name).allMatch(e -> Arrays.asList(subsetTypes).contains(e));
    } else {
      throw new NotImplementedException("type not implemented:" + types.getClass().getTypeName());
    }
  }
}
