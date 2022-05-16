package org.dcsa.core.extendedrequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.exception.ConcreteRequestErrorMessageException;
import org.dcsa.core.query.DBEntityAnalysis;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.relational.core.sql.*;
import org.springframework.r2dbc.core.binding.BindMarker;
import org.springframework.r2dbc.core.binding.MutableBindings;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class QueryParameterParser<T> {
  private static final String PARAMETER_SPLIT = "&";
  private static final String SORT_SEPARATOR = ",";
  public static final FilterCondition EMPTY_CONDITION = InlineableFilterCondition.of(TrueCondition.INSTANCE);

  @NonNull
  protected final ExtendedParameters extendedParameters;
  protected final R2dbcDialect r2dbcDialect;
  protected final DBEntityAnalysis<T> dbAnalysis;
  protected final List<FilterCondition> filters = new ArrayList<>();
  protected final List<OrderByField> orderByFields = new ArrayList<>();
  private final Map<String, List<String>> parsedParameters = new LinkedHashMap<>();
  protected final Set<QueryField> referencedFields = new HashSet<>();

  @Getter(lazy = true)
  private final Map<String, ParserFunction> specialAttributes = generateSpecialAttributesTable();
  private int limit = -1;
  private int offset = 0;

  private Map<String, ParserFunction> generateSpecialAttributesTable() {
    assert extendedParameters != null;
    return Map.of(
      extendedParameters.getPaginationCursorName(), this::parseCursorQueryParameter,
      extendedParameters.getSortParameterName(), this::parseSortQueryParameter,
      extendedParameters.getPaginationPageSizeName(), this::parseLimitQueryParameter,
      extendedParameters.getIndexCursorName(),  this::parseOffsetQueryParameter
    );
  }

  private MutableBindings mutableBindings;

  private ParseState parseState = ParseState.INITIAL;

  protected MutableBindings getMutableBindings() {
    if (mutableBindings == null) {
      mutableBindings = new MutableBindings(r2dbcDialect.getBindMarkersFactory().create());
    }
    return mutableBindings;
  }

  public CursorBackedFilterCondition build() {
    // Note: We always process default parameters because:
    // 1) it makes the cursor smaller if we do not include defaults
    // 2) it avoids us having to deal with the problem that the defaults do not have to be a "valid"
    //    value for the user to provide (e.g., internal-only fields) but the cursor is user provided
    //    (and user controlled too).
    handleDefaultParameters();
    parseState = parseState.endParsing();
    return DelegatingCursorBackedFilterCondition.of(
      andAllFilters(filters, false),
      immutableCopy(parsedParameters),
      Collections.unmodifiableSet(referencedFields),
      Collections.unmodifiableList(orderByFields),
      getMutableBindings(),
      limit,
      offset
    );
  }

  private void handleDefaultParameters() {
    parseState = parseState.startParseDefaultsRound();
    for (QueryField queryField : dbAnalysis.getQueryFields()) {
      QueryFieldRestriction restriction = dbAnalysis.getQueryFieldRestriction(queryField);
      if (restriction == null || !restriction.hasDefault() || !referencedFields.add(queryField)) {
        continue;
      }
      assert restriction.getDefaultValue() != null && !restriction.getDefaultValue().isEmpty();
      parseSingleParameter(queryField.getJsonName(), queryField.getJsonName(), null, restriction.getDefaultValue(), false, false);
    }
    if (limit == -1) {
      limit = extendedParameters.getDefaultPageSize();
    }
    assert offset >= 0;
    parseState = parseState.finishParsingRound();
  }

  private static Map<String, List<String>> immutableCopy(Map<String, List<String>> orig) {
    LinkedHashMap<String, List<String>> copy = new LinkedHashMap<>(orig.size());
    for (Map.Entry<String, List<String>> entry : orig.entrySet()) {
      copy.put(entry.getKey(), List.copyOf(entry.getValue()));
    }
    return Collections.unmodifiableMap(copy);
  }

  private void parseSingleParameter(String parameterKey, String jsonName, String fieldAttribute, List<String> values, boolean validateValue, boolean recordInCursor) {
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
    parseState = parseState.parsedArgument(extendedParameters.getPaginationCursorName());
    if (recordInCursor) {
      recordAttributeInCursor(parameterKey, values);
    }
  }

  private void queryParameterHasExactlyOneValue(String parameterKey, List<String> values) {
    if (values.size() != 1) {
      throw ConcreteRequestErrorMessageException.invalidQuery(parameterKey, "param " + parameterKey + " was repeated but should only appear once");
    }
  }

  private void parseOffsetQueryParameter(String parameterKey, String jsonName, String fieldAttribute, List<String> values) {
    noFieldAttributeAllowed(jsonName, fieldAttribute);
    queryParameterHasExactlyOneValue(parameterKey, values);
    // Skip recordAttributeInCursor(parameterKey, values) - this field is computed based on which "page" you go to
    if (parseState != ParseState.PARSING_CURSOR) {
      throw ConcreteRequestErrorMessageException.invalidQuery(extendedParameters.getPaginationCursorName(),
        "Unknown field " + parameterKey + " (only valid inside a cursor)");
    }

    try {
      offset = Integer.parseInt(values.get(0));
    } catch (NumberFormatException e) {
      offset = -1;
    }
    // Either we got a non-int or a negative integer - either way is useless and someone attempting
    // to mess with our cursor implementation.
    if (offset < 0) {
      throw ConcreteRequestErrorMessageException.invalidQuery(extendedParameters.getPaginationCursorName(),
        "Please do not fiddle with the cursor argument (invalid offset).");
    }
    parseState = parseState.parsedArgument(extendedParameters.getPaginationCursorName());
  }

  private void parseCursorQueryParameter(String parameterKey, String jsonName, String fieldAttribute, List<String> values) {
    noFieldAttributeAllowed(jsonName, fieldAttribute);
    parseState = parseState.parsingCursor(parameterKey);
    // Skip recordAttributeInCursor(parameterKey, values) - we do not want to include the cursor inside itself.
    queryParameterHasExactlyOneValue(parameterKey, values);
    byte[] decodedCursor = Base64.getUrlDecoder().decode(values.get(0));
    Map<String, List<String>> params = convertToQueryStringToHashMap(new String(decodedCursor, StandardCharsets.UTF_8));
    parseQueryParameter(params);
    // the parseQueryParameter method will update the parsing round for us.
    // parseState = parseState.finishParsingRound();
    assert parseState == ParseState.PARSED_CURSOR;
  }

  private void parseLimitQueryParameter(String parameterKey, String jsonName, String fieldAttribute, List<String> values) {
    noFieldAttributeAllowed(jsonName, fieldAttribute);
    queryParameterHasExactlyOneValue(parameterKey, values);
    recordAttributeInCursor(parameterKey, values);

    String value = values.get(0);
    try {
      if ("ALL".equals(value)) {
        limit = 0;
      } else {
        limit = Integer.parseInt(value);
        if (limit < 1) {
          throw ConcreteRequestErrorMessageException.invalidQuery(parameterKey,
            "Invalid " + parameterKey + " value: " + value + ". Must be at least 1");
        }
      }
    } catch (NumberFormatException numberFormatException) {
      throw ConcreteRequestErrorMessageException.invalidQuery(parameterKey,
        "Unknown " + parameterKey + " value: " + value + ". Must be a Number!");
    }
    if (extendedParameters.getMaxPageSize() != 0 && (limit == 0 || limit > extendedParameters.getMaxPageSize())) {
      throw ConcreteRequestErrorMessageException.invalidQuery(parameterKey,
        "Invalid " +  parameterKey + " value: " + value + ". Max page is: " + extendedParameters.getMaxPageSize());
    }
    parseState = parseState.parsedArgument(extendedParameters.getPaginationCursorName());
  }

  private void parseSortQueryParameter(String parameterKey, String jsonName, String fieldAttribute, List<String> values) {
    noFieldAttributeAllowed(jsonName, fieldAttribute);
    queryParameterHasExactlyOneValue(parameterKey, values);
    recordAttributeInCursor(parameterKey, values);

    Map<String, Sort.Direction> sortDirections = Map.of(
      extendedParameters.getSortDirectionAscendingName(), Sort.Direction.ASC,
      extendedParameters.getSortDirectionDescendingName(), Sort.Direction.DESC
    );
    String[] sortableValues = values.get(0).split(SORT_SEPARATOR);
    for (String sortableValue : sortableValues) {
      String[] fieldAndDirection = sortableValue.split(extendedParameters.getSortDirectionSeparator());
      String sortFieldJsonName = fieldAndDirection[0];
      // Verify that the field exists on the model class and transform it from JSON-name to FieldName
      QueryField queryField = getQueryFieldFromJSONName(sortFieldJsonName);

      // Use select name where possible due to
      // https://github.com/spring-projects/spring-data-jdbc/issues/968
      Column column = queryField.getSelectColumn();
      if (column == null) {
        column = queryField.getInternalQueryColumn();
      }

      Sort.Direction sortDirection;
      switch (fieldAndDirection.length) {
        case 1:
          sortDirection = Sort.Direction.ASC;
          break;
        case 2:
          sortDirection = sortDirections.get(fieldAndDirection[1]);
          if (sortDirection != null) {
            break;
          }
          // Fall-through into the default for invalid directions
        default:
          throw ConcreteRequestErrorMessageException.invalidQuery(jsonName,
            "Sort parameter not correctly specified. Use - {fieldName} " + extendedParameters.getSortDirectionSeparator() + "[ASC|DESC]");
      }
      orderByFields.add(OrderByField.from(column, sortDirection));
    }
    parseState = parseState.parsedArgument(extendedParameters.getPaginationCursorName());
  }

  public void parseQueryParameter(Map<String, List<String>> queryParameters) {
    Map<String, ParserFunction> specialAttributesTable = generateSpecialAttributesTable();
    ParserFunction defaultParser = (pk, jn, fa, v) -> parseSingleParameter(pk, jn, fa, v, true, true);
    Set<String> reservedParameters = Set.copyOf(extendedParameters.getReservedParameters());
    for (Map.Entry<String, List<String>> queryParameter : queryParameters.entrySet()) {
      final String parameterKey = queryParameter.getKey();
      List<String> values = queryParameter.getValue();
      String jsonName = parameterKey;
      String fieldAttribute;
      if (reservedParameters.contains(parameterKey)) {
        continue;
      }
      String separator = extendedParameters.getQueryParameterAttributeSeparator();
      int index = parameterKey.indexOf(separator);
      if (index > -1) {
        jsonName = parameterKey.substring(0, index);
        if (reservedParameters.contains(jsonName)) {
          continue;
        }
        fieldAttribute = parameterKey.substring(index + separator.length());
      } else {
        fieldAttribute = null;
      }
      ParserFunction parserFunction = specialAttributesTable.getOrDefault(jsonName, defaultParser);
      parserFunction.parseParameter(parameterKey, jsonName, fieldAttribute, values);
    }
    parseState = parseState.finishParsingRound();
  }


  private static void noFieldAttributeAllowed(String jsonName, String fieldAttribute) {
    if (fieldAttribute != null) {
      throw ConcreteRequestErrorMessageException.invalidQuery(jsonName, "The field " + jsonName
        + " do not support attributes.  Please remove the \":" + fieldAttribute + "\" from the parameter.");
    }
  }

  private static Map<String, List<String>> convertToQueryStringToHashMap(String source) {
    Map<String, List<String>> data = new LinkedHashMap<>();
    final String[] arrParameters = source.split(PARAMETER_SPLIT);
    for (final String tempParameterString : arrParameters) {
      final String[] arrTempParameter = tempParameterString.split("=");
      final String parameterKey = arrTempParameter[0];
      final List<String> valueList = data.computeIfAbsent(parameterKey, k -> new ArrayList<>());
      if (arrTempParameter.length >= 2) {
        final String parameterValue = arrTempParameter[1];
        valueList.add(parameterValue);
      } else {
        valueList.add("");
      }
    }
    return data;
  }

  private void recordAttributeInCursor(String parameterKey, List<String> values) {
    parsedParameters.computeIfAbsent(parameterKey, ignored -> new ArrayList<>(values.size())).addAll(values);
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
          "Cannot filter/order by on field: " + jsonName + " (field is ignored)");
      }
      if (javaField.isAnnotationPresent(Transient.class)) {
        throw ConcreteRequestErrorMessageException.invalidQuery(jsonName,
          "Cannot filter/order on field: " + jsonName + " (field is transient)");
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

}
