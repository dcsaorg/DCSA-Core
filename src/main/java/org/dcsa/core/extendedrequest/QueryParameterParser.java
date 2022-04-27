package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.exception.ConcreteRequestErrorMessageException;
import org.dcsa.core.query.DBEntityAnalysis;
import org.springframework.data.annotation.Transient;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.*;
import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.Bindings;
import org.springframework.r2dbc.core.binding.MutableBindings;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class QueryParameterParser<T> {

  public static final FilterCondition EMPTY_CONDITION = InlineableFilterCondition.of(TrueCondition.INSTANCE);

  protected final ExtendedParameters extendedParameters;
  protected final R2dbcDialect r2dbcDialect;
  protected final DBEntityAnalysis<T> dbAnalysis;
  protected final List<FilterCondition> filters = new ArrayList<>();
  private final Map<String, List<String>> parsedParameters = new LinkedHashMap<>();
  protected final Set<QueryField> referencedFields = new HashSet<>();

  private MutableBindings mutableBindings;
  private boolean used = false;

  protected MutableBindings getMutableBindings() {
    if (mutableBindings == null) {
      mutableBindings = new MutableBindings(r2dbcDialect.getBindMarkersFactory().create());
    }
    return mutableBindings;
  }

  public CursorBackedFilterCondition build() {
    if (used) {
      throw new IllegalStateException("build must only be called once");
    }
    used = true;
    handleDefaultParameters();
    return DelegatingCursorBackedFilterCondition.of(
      andAllFilters(filters, false),
      immutableCopy(parsedParameters),
      Collections.unmodifiableSet(referencedFields),
      getMutableBindings()
    );
  }

  private void handleDefaultParameters() {
    for (QueryField queryField : dbAnalysis.getQueryFields()) {
      QueryFieldRestriction restriction = dbAnalysis.getQueryFieldRestriction(queryField);
      if (restriction == null || !restriction.hasDefault() || !referencedFields.add(queryField)) {
        continue;
      }
      assert restriction.defaultValue() != null && !restriction.defaultValue().isEmpty();
      parseSingleParameter(queryField.getJsonName(), queryField.getJsonName(), null, restriction.defaultValue(), false);
    }
  }

  private static Map<String, List<String>> immutableCopy(Map<String, List<String>> orig) {
    LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>(orig.size());
    for (Map.Entry<String, List<String>> entry : orig.entrySet()) {
      copy.put(entry.getKey(), List.copyOf(entry.getValue()));
    }
    return Collections.unmodifiableMap(copy);
  }

  private void parseSingleParameter(String parameterKey, String jsonName, String fieldAttribute, List<String> values, boolean validateValue) {
    if (values.isEmpty()) {
      throw new IllegalArgumentException("No values provided for " + parameterKey);
    }
    QueryField queryField = getQueryFieldFromJSONName(jsonName);
    if (validateValue) {
      QueryFieldRestriction queryFieldRestriction = dbAnalysis.getQueryFieldRestriction(queryField);
      if (queryFieldRestriction != null) {
        queryFieldRestriction.validateValues(queryField, values);
      }
    }
    ComparisonType comparisonType = parseComparisonType(queryField, fieldAttribute);
    Function<String, Expression> value2BindVariable = v -> this.bindQueryParameterValue(queryField, comparisonType, fieldAttribute, v);
    FilterCondition filterCondition = queryField.generateCondition(
      comparisonType,
      fieldAttribute,
      values,
      value2BindVariable
    );
    if (filterCondition != null) {
      filters.add(filterCondition);
    } else {
      if (values.size() != 1) {
        throw ConcreteRequestErrorMessageException.invalidQuery(parameterKey,
          "Cannot have " + parameterKey + " twice: Feature is not implemented");
      }
      parseSingleValueQueryParameter(queryField, comparisonType, fieldAttribute, values.get(0));
    }
    parsedParameters.computeIfAbsent(parameterKey, ignored -> new ArrayList<>()).addAll(values);
  }

  public void parseQueryParameter(Map<String, List<String>> queryParameters, Predicate<String> ignoredParameter) {
    for (Map.Entry<String, List<String>> queryParameter : queryParameters.entrySet()) {
      final String parameterKey = queryParameter.getKey();
      List<String> values = queryParameter.getValue();
      String jsonName = parameterKey;
      String fieldAttribute;
      if (ignoredParameter.test(parameterKey)) {
        continue;
      }
      String separator = extendedParameters.getQueryParameterAttributeSeparator();
      int index = parameterKey.indexOf(separator);
      if (index > -1) {
        jsonName = parameterKey.substring(0, index);
        if (ignoredParameter.test(jsonName)) {
          continue;
        }
        fieldAttribute = parameterKey.substring(index + separator.length());
      } else {
        fieldAttribute = null;
      }
      parseSingleParameter(parameterKey, jsonName, fieldAttribute, values, true);
    }
  }

  protected Expression bindQueryParameterValue(QueryField queryField, ComparisonType comparisonType, String fieldAttribute, String value) {
    Object parsedValue = parseValue(queryField, comparisonType, fieldAttribute, value);
    return bindValue(queryField, parsedValue);
  }

  protected Expression bindValue(QueryField queryField, Object value) {
    referencedFields.add(queryField);
    MutableBindings bindings = getMutableBindings();
    BindMarker marker = bindings.nextMarker(queryField.getJsonName());
    bindings.bind(marker, value);
    return SQL.bindMarker(marker.getPlaceholder());
  }

  protected boolean handleIsNullQuery(QueryField queryField, String attribute, ComparisonType comparisonType, String value) {
    if ("NULL".equals(value)) {
      if (attribute == null) {
        comparisonType = ComparisonType.EQ;
      } else if (comparisonType != ComparisonType.EQ && comparisonType != ComparisonType.NEQ) {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Cannot use attribute (operator) " + attribute + " on " + queryField.getJsonName()
            + " with null value: Null values can only be used with strictly equal or not equal");
      }
      Condition condition = Conditions.isNull(comparisonType.defaultFieldConversion(queryField));
      if (comparisonType == ComparisonType.NEQ) {
        condition = condition.not();
      }
      filters.add(InlineableFilterCondition.of(condition));
      return true;
    }
    return false;
  }

  protected ComparisonType parseComparisonType(QueryField queryField, String fieldAttribute) {
    if (fieldAttribute != null) {
      try {
        return ComparisonType.valueOf(fieldAttribute.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Attribute (operator) " + fieldAttribute + " is invalid for " + queryField.getJsonName());
      }
    }
    return ComparisonType.EQ;
  }

  protected void parseSingleValueQueryParameter(QueryField queryField, ComparisonType comparisonType, String fieldAttribute, String value) {
    Class<?> fieldType = queryField.getType();

    if (handleIsNullQuery(queryField, fieldAttribute, comparisonType, value)) {
      return;
    }

    if (value == null || value.equals("")) {
      throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
        "Cannot use empty value as filter for " + queryField.getJsonName() + ".");
    }

    if (fieldType.isEnum()) {
      // Return type IS Enum - split a possible list on EnumSplitter (since this is an Enum - no special characters are allowed). If
      // enumSplit value is a "," or a "|" this will work fine
      String[] enumList = value.split(extendedParameters.getEnumSplit());
      if (fieldAttribute == null) {
        comparisonType = ComparisonType.EQ;
      }
      if (comparisonType.isRequiredOrdering()) {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Cannot use attribute (operator) " + fieldAttribute + " on " + queryField.getJsonName()
            + ": It does not have an ordering but the operator needs ordering");
      }
      if (enumList.length == 1) {
        filters.add(comparisonType.singleNonNullValueCondition(queryField, bindValue(queryField, enumList[0])));
      } else {
        filters.add(comparisonType.multiValueCondition(queryField,
          Arrays.stream(enumList).map(v -> bindValue(queryField, v)).collect(Collectors.toList())
        ));
      }
      return;
    }
    Object parsed = parseValue(queryField, comparisonType, fieldAttribute, value);
    filters.add(comparisonType.singleNonNullValueCondition(queryField, bindValue(queryField, parsed)));
  }

  protected Object parseValue(QueryField queryField, ComparisonType comparisonType, String fieldAttribute, String value) {
    Class<?> fieldType = queryField.getType();
    Object parsed;

    if (String.class.equals(fieldType) || fieldType.isEnum()) {
      // Enums are basically handled as strings as far as values are concerned if we get here.  Though there is
      // a special-case with comma handling but that is handled before getting here.
      parsed = value;
    } else if (UUID.class.equals(fieldType)) {
      if (comparisonType.isRequiredOrdering()) {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Cannot use attribute (operator) " + fieldAttribute + " on " + queryField.getJsonName()
            + ": It does not have an ordering but the operator needs ordering");
      }
      if (comparisonType != ComparisonType.EQ) {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Cannot use attribute (operator) " + fieldAttribute + " on " + queryField.getJsonName()
            + ": UUID values can only be used with strictly equal");
      }
      try {
        parsed = UUID.fromString(value);
      } catch (IllegalArgumentException e) {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Cannot filter on " + queryField.getJsonName() + ": The value \"" + value
            + "\" could not be parsed as an UUID");
      }
    } else if (Temporal.class.isAssignableFrom(fieldType)) {
      String dateFormat = queryField.getDatePattern();
      if (dateFormat != null && !dateFormat.equals("")) {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Cannot filter on " + queryField.getJsonName() + ": It uses a custom date pattern");
      }

      try {
        if (OffsetDateTime.class.equals(fieldType)) {
          parsed = OffsetDateTime.parse(value);
        } else if (LocalDate.class.equals(fieldType)) {
          parsed = LocalDate.parse(value);
        } else if (LocalDateTime.class.equals(fieldType)) {
          parsed = LocalDateTime.parse(value);
        } else {
          throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
            "Cannot filter on " + queryField.getJsonName() + ": Unsupported Temporal class");
        }
      } catch (DateTimeParseException e) {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Cannot filter on " + queryField.getJsonName() + ": The value \"" + value
            + "\" could not be parsed in the correct date format");
      }
    } else if (Number.class.isAssignableFrom(fieldType)) {
      if (Long.class.equals(fieldType)) {
        parsed = Long.parseLong(value);
      } else if (Integer.class.equals(fieldType)) {
        parsed = Integer.parseInt(value);
      } else if (BigDecimal.class.equals(fieldType)) {
        parsed = new BigDecimal(value);
      } else {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Cannot filter on " + queryField.getJsonName()
            + ": Numeric format " + fieldType.getTypeName() + " is not supported");
      }
    } else if (Boolean.class.equals(fieldType)) {
      if ("TRUE".equalsIgnoreCase(value)) {
        parsed = Boolean.TRUE;
      } else if ("FALSE".equalsIgnoreCase(value)) {
        parsed = Boolean.FALSE;
      } else {
        throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
          "Boolean filter value must be either: (TRUE|FALSE) - value not recognized: " + value + " on filter: " + fieldType.getSimpleName());
      }
    } else {
      throw ConcreteRequestErrorMessageException.invalidQuery(queryField.getJsonName(),
        "Type on filter (" + queryField.getJsonName() + ") not recognized: " + fieldType.getSimpleName());
    }
    return parsed;
  }

  protected QueryField getQueryFieldFromJSONName(String jsonName) {
    QueryField queryField;
    Field javaField;
    try {
      queryField = dbAnalysis.getQueryFieldFromJSONName(jsonName);
    } catch (IllegalArgumentException e) {
      throw ConcreteRequestErrorMessageException.invalidQuery(jsonName,
        "Unknown parameter/field name: " + jsonName);
    }
    javaField = queryField.getCombinedModelField();
    if (javaField != null) {
      if (javaField.isAnnotationPresent(JsonIgnore.class)) {
        throw ConcreteRequestErrorMessageException.invalidQuery(jsonName,
          "Cannot filter on field: " + jsonName + " (field is ignored)");
      }
      if (javaField.isAnnotationPresent(Transient.class)) {
        throw ConcreteRequestErrorMessageException.invalidQuery(jsonName,
          "Cannot filter on field: " + jsonName + " (field is transient)");
      }
    }
    referencedFields.add(queryField);
    return queryField;
  }

  static FilterCondition isubstr(Expression lhs, Expression rhsOrig) {
    Expression rhs = surroundWithWildCards(rhsOrig);
    return r2dbcDialect -> {
      if (r2dbcDialect instanceof PostgresDialect) {
        return Comparison.create(lhs, "ILIKE", rhs);
      }
      return Conditions.like(Functions.upper(lhs), Functions.upper(rhs));
    };
  }

  static FilterCondition iequal(Expression lhs, Expression rhs) {
    return InlineableFilterCondition.of(Conditions.isEqual(Functions.upper(lhs), Functions.upper(rhs)));
  }

  private static Expression surroundWithWildCards(Expression rhs) {
    List<Expression> params = List.of(
      SQL.literalOf("%"),
      rhs,
      SQL.literalOf("%")
    );
    return SimpleFunction.create("CONCAT", params);
  }

  static FilterCondition andAllFilters(List<FilterCondition> filters, boolean nestOnMultiple) {
    if (filters.isEmpty()) {
      return EMPTY_CONDITION;
    }
    if (filters.size() == 1) {
      return filters.get(0);
    }
    if (filters.stream().allMatch(FilterCondition::isInlineable)) {
      return InlineableFilterCondition.of(andAll(filters.stream().map(FilterCondition::computeCondition), nestOnMultiple));
    }
    return (r2dbcDialect) -> andAll(filters.stream().map(f -> f.computeCondition(r2dbcDialect)), nestOnMultiple);
  }

  private static Condition andAll(Stream<Condition> conditions, boolean nestOnMultiple) {
    return andAll(conditions::iterator, nestOnMultiple);
  }

  private static Condition andAll(Iterable<Condition> conditions, boolean nestOnMultiple) {
    Iterator<Condition> iter = conditions.iterator();
    assert iter.hasNext();
    Condition con = iter.next();
    if (!iter.hasNext()) {
      return con;
    }
    do {
      con = con.and(iter.next());
    } while (iter.hasNext());
    if (!nestOnMultiple) {
      return con;
    }
    return Conditions.nest(con);
  }

  @RequiredArgsConstructor(staticName = "of")
  static class InlineableFilterCondition implements FilterCondition {
    private final Condition condition;

    public Condition computeCondition(R2dbcDialect r2dbcDialect) {
      return condition;
    }

    public boolean isInlineable() {
      return true;
    }
  }

  @RequiredArgsConstructor(staticName = "of")
  private static class DelegatingCursorBackedFilterCondition implements CursorBackedFilterCondition {

    private final FilterCondition delegate;

    @Getter
    private final Map<String, List<String>> cursorParameters;

    @Getter
    private final Set<QueryField> referencedQueryFields;

    @Getter
    private final Bindings bindings;

    @Override
    public Condition computeCondition(R2dbcDialect r2dbcDialect) {
      return delegate.computeCondition(r2dbcDialect);
    }

    @Override
    public boolean isInlineable() {
      return delegate.isInlineable();
    }
  }
}
