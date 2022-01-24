package org.dcsa.core.validator;

import org.dcsa.core.util.ReflectUtility;

import javax.el.MethodNotFoundException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.temporal.Temporal;

/**
 * Validator for Enumeration subsets. Works both for queryParameters (where the value to validate is
 * a string) and for Enum defined fields (where the value to validate is an Enum)
 *
 * <p>If the value is a string - then this class supports that the string can be comma separated in
 * order to validate a set of values
 */
public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {

  private String startFieldName;
  private String endFieldName;
  private DateRange.NullHandling nullHandling;
  private DateRange.EqualHandling equalHandling;

  @Override
  public void initialize(DateRange constraintAnnotation) {
    this.startFieldName = constraintAnnotation.startField();
    this.endFieldName = constraintAnnotation.endField();
    this.nullHandling = constraintAnnotation.nullHandling();
    this.equalHandling = constraintAnnotation.equalHandling();
  }

  @Override
  public boolean isValid(Object entity, ConstraintValidatorContext constraintValidatorContext) {
    if (null == entity) {
      return true;
    }
    return this.isValidImpl(entity, constraintValidatorContext);
  }

  private <T extends Comparable<T>> boolean isValidImpl(Object entity, ConstraintValidatorContext constraintValidatorContext) {
    Field startField, endField;
    Method startFieldGetter, endFieldGetter;
    T startDateValue, endDateValue;
    Class<?> dateType;
    try {
      startField = ReflectUtility.getDeclaredField(entity.getClass(), startFieldName);
      endField = ReflectUtility.getDeclaredField(entity.getClass(), endFieldName);

      startFieldGetter = ReflectUtility.getGetterMethodFromName(entity.getClass(), startFieldName);
      endFieldGetter = ReflectUtility.getGetterMethodFromName(entity.getClass(), endFieldName);
    } catch (NoSuchFieldException|MethodNotFoundException e) {
      throw new IllegalStateException("Invalid @DateRange annotation for entity " + entity.getClass().getSimpleName()
              + ": The entity does not have both of the fields " + startFieldName + " and " + endFieldName, e);
    }
    dateType = startField.getType();
    if (!Temporal.class.isAssignableFrom(dateType) || !Comparable.class.isAssignableFrom(dateType)) {
      throw new IllegalStateException("Invalid @DateRange annotation for entity " + entity.getClass().getSimpleName()
              + ": The type of " + startFieldName + " must be a Comparable & Temporal (such as ZonedDateTime)");
    }
    if (!dateType.equals(startFieldGetter.getReturnType())) {
      throw new IllegalStateException("Invalid @DateRange annotation for entity " + entity.getClass().getSimpleName()
              + ": The field and getter methods disagree on the return type for " + startFieldName);
    }
    if (!dateType.equals(endField.getType())) {
      throw new IllegalStateException("Invalid @DateRange annotation for entity " + entity.getClass().getSimpleName()
              + ": The fields " + startFieldName + " and " + endFieldName + " must have the same type");
    }
    if (!startFieldGetter.getReturnType().equals(endFieldGetter.getReturnType())) {
      throw new IllegalStateException("Invalid @DateRange annotation for entity " + entity.getClass().getSimpleName()
              + ": The getters for " + startFieldName + " and " + endFieldName + " must have the same return type");
    }
    try {
      startDateValue = cast(startFieldGetter.invoke(entity));
      endDateValue = cast(endFieldGetter.invoke(entity));
    } catch (IllegalAccessException|InvocationTargetException e) {
      throw new IllegalStateException("Issue with @DateRange annotation for entity " + entity.getClass().getSimpleName()
              + ": The getter for " + startFieldName + " or " + endFieldName + " triggered an exception!");
    }

    if (startDateValue == null || endDateValue == null) {
      if (startDateValue == null && endDateValue == null) {
        return this.nullHandling == DateRange.NullHandling.BOTH_CAN_BE_NULL;
      }
      return true;
    }

    int comparison = startDateValue.compareTo(endDateValue);
    if (comparison == 0) {
      return this.equalHandling == DateRange.EqualHandling.EQUAL_OK;
    }
    return comparison < 0;
  }

  // Helper to deal compiler warnings about unchecked casts.
  private static <T> T cast(Object value) {
    @SuppressWarnings("unchecked")
    T t = (T)value;
    return t;
  }
}
