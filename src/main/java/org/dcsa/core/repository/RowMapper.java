package org.dcsa.core.repository;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.dcsa.core.exception.DatabaseException;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.util.MappingUtils;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.annotation.Id;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

class RowMapper {
  private static final Integer DATABASE_INTERVAL_NATIVE_TYPE = 1186;

  private static final Pattern LAST_SEGMENT = Pattern.compile("[.][^.]++$");

  private final ObjectMapper objectMapper =
    new ObjectMapper()
      .registerModule(new JavaTimeModule()) // Needed to process time objects.
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
      .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
      .setAnnotationIntrospector(
        new JacksonAnnotationIntrospector() {
          @Override
          protected <A extends Annotation> A _findAnnotation(
            Annotated annotated, Class<A> annoClass) {
            if (JsonIgnore.class.equals(annoClass)) {
              return null;
            }
            A annotation = super._findAnnotation(annotated, annoClass);

            if (annotation != null && JsonProperty.class.equals(annoClass)) {
              annotation = MappingUtils.jsonPropertyAutoMapper(annotation);
            }
            return annotation;
          }
        });

  public <T> T mapRow(Row row, RowMetadata metadata, DBEntityAnalysis<T> dbEntityAnalysis, Class<T> modelClass, boolean ignoreUnknownProperties) {
    Map<String, Object> objectMap = objectMapFromRow(row, metadata, dbEntityAnalysis, ignoreUnknownProperties);
    return objectMapper.convertValue(objectMap, modelClass);
  }

  private static <T> Map<String, Object> objectMapFromRow(Row row, RowMetadata metadata, DBEntityAnalysis<T> dbEntityAnalysis, boolean ignoreUnknownProperties) {
    Map<String, Object> objectMap = new HashMap<>();
    Map<String, Field> fieldMap = new HashMap<>();
    for (String columnName : metadata.getColumnNames()) {
      Field modelField;
      try {
        modelField = getCombinedModelField(columnName, dbEntityAnalysis);
      } catch (DatabaseException exception) {
        if (ignoreUnknownProperties) {
          continue;
        }
        throw exception;
      }
      fieldMap.put(columnName, modelField);
      // Use the ID fields to tell if a given entity was matched.  They can be omitted due to LEFT JOINs and then
      // we would rather the whole object is null than an entity where all the fields are set to null.
      Object value = row.get(columnName);
      boolean isIDField = modelField.isAnnotationPresent(Id.class);
      if (!isIDField) {
        continue;
      }

      if (value != null) {
        // ID is not null, create a placeholder for the entity, so we can rely on the presence of the entity
        // in the object map to determine whether the entity should be mapped.
        lookupSubmapForColumnName(objectMap, columnName, true);
      }
    }

    for (String columnName : metadata.getColumnNames()) {
      Field modelField = fieldMap.get(columnName);
      if (modelField == null) {
        continue;
      }
      Class<?> fieldType = modelField.getType();

      Object value;
      ColumnMetadata columnMetadata = metadata.getColumnMetadata(columnName);
      Class<?> columnClass = columnMetadata.getJavaType();
      // Handle Database Intervals as Java String type
      if (columnClass == null && !DATABASE_INTERVAL_NATIVE_TYPE.equals(columnMetadata.getNativeTypeMetadata())) {
        throw new DatabaseException("Type for columnName " + columnName + " is null");
      }
      value = row.get(columnName);

      if (fieldType.isEnum() && value instanceof String) {
        value = parseEnum(fieldType, (String) value);
      }
      Map<String, Object> entityMap = lookupSubmapForColumnName(objectMap, columnName);
      if (entityMap == null) {
        // Entity is not mapped (e.g., because it was involved in a LEFT JOIN and the match failed).
        // Note for this to happen, the value itself must also be null
        assert value == null : columnName + " as not null, but could not find the ID column for the entity it belongs to!";
        continue;
      }
      entityMap.put(ReflectUtility.transformFromFieldNameToJsonName(modelField), value);
    }

    return objectMap;
  }

  private static Map<String, Object> lookupSubmapForColumnName(Map<String, Object> objectMap, String columnName) {
    return lookupSubmapForColumnName(objectMap, columnName, false);
  }

  private static Map<String, Object> lookupSubmapForColumnName(Map<String, Object> objectMap, String columnName, boolean createIfMissing) {
    Map<String, Object> lastMap = objectMap;
    // Create maps at each "." if not existing already.
    if (columnName.contains(".")) {
      String[] keys = columnName.split("\\.");
      // Create maps for preceding keys. The last key is the actual field.
      for (int i=0; i < keys.length-1; i++) {
        String key = keys[i];
        Map<String, Object> currentMap;
        if (lastMap.containsKey(key)) {
          Object currentValue = lastMap.get(key);
          if (currentValue instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) currentValue;
            currentMap = m;
          } else {
            throw new IllegalStateException("Key: " + key + " in columnName: " + columnName
              + " is already set to a value of class " + currentValue.getClass().getSimpleName()
              + ", but expected to be a Map. Is the value for the key set twice?");
          }
        }
        else {
          if (!createIfMissing) {
            return null;
          }
          currentMap = new HashMap<>();
          lastMap.put(key, currentMap);
        }

        lastMap = currentMap;
      }
    }
    return lastMap;
  }

  private static <T> Field getCombinedModelField(String selectName, DBEntityAnalysis<T> dbEntityAnalysis) {
    QueryField dbField;
    try {
      dbField = dbEntityAnalysis.getQueryFieldFromSelectName(selectName);
    } catch (IllegalArgumentException exception) {
      throw new DatabaseException("Query field is unavailable for selectName: " + selectName, exception);
    }
    Field combinedModelField = dbField.getCombinedModelField();
    if (combinedModelField == null) {
      throw new IllegalStateException("Internal error: Attempting to get combined model field without a backing field!?");
    }
    return combinedModelField;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static Enum<?> parseEnum(Class<?> enumClass, String value) {
    return Enum.valueOf((Class) enumClass, value);
  }
}
