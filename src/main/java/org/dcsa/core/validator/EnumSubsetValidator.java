package org.dcsa.core.validator;

import org.apache.commons.lang3.NotImplementedException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validator for Enumeration subsets. Works both for queryParameters (where the value to validate is
 * a string) and for Enum defined fields (where the value to validate is an Enum)
 *
 * <p>If the value is a string - then this class supports that the string can be comma separated in
 * order to validate a set of values
 */
public class EnumSubsetValidator implements ConstraintValidator<EnumSubset, Object> {

  private Set<String> subsetTypes;

  @Override
  public void initialize(EnumSubset constraintAnnotation) {
    this.subsetTypes = Arrays.stream(constraintAnnotation.anyOf())
            .map(s -> s.split(","))
            .flatMap(Arrays::stream)
            .collect(Collectors.toSet());
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
      return enumStream.allMatch(e -> subsetTypes.contains(e));
    } else if (types instanceof List) {
      @SuppressWarnings("unchecked")
      List<? extends Enum<?>> enumList = (List<? extends Enum<?>>) types;
      return enumList.stream()
          .map(Enum::name)
          .allMatch(subsetTypes::contains);
    } else if (types instanceof Enum) {
      return subsetTypes.contains(((Enum<?>) types).name());
    } else {
      throw new NotImplementedException("type not implemented:" + types.getClass().getTypeName());
    }
  }
}
