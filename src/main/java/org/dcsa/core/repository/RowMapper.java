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
import org.dcsa.core.util.ReflectUtility;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class RowMapper {
    private static final Integer DATABASE_INTERVAL_NATIVE_TYPE = 1186;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()) // Needed to process time objects.
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
            .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
            .setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
                @Override
                protected <A extends Annotation> A _findAnnotation(Annotated annotated, Class<A> annoClass) {
                    if (JsonIgnore.class.equals(annoClass)) {
                        return null;
                    }
                    return super._findAnnotation(annotated, annoClass);
                }
            });

    public <T> T mapRow(Row row, RowMetadata metadata, DBEntityAnalysis<T> dbEntityAnalysis, Class<T> modelClass, boolean ignoreUnknownProperties) {
        Map<String, Object> objectMap = objectMapFromRow(row, metadata, dbEntityAnalysis, ignoreUnknownProperties);
        return objectMapper.convertValue(objectMap, modelClass);
    }

    private static <T> Map<String, Object> objectMapFromRow(Row row, RowMetadata metadata, DBEntityAnalysis<T> dbEntityAnalysis, boolean ignoreUnknownProperties) {
        Map<String, Object> objectMap = new HashMap<>();

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
            Class<?> fieldType = modelField.getType();

            Object value;
            ColumnMetadata columnMetadata = metadata.getColumnMetadata(columnName);
            Class<?> columnClass = columnMetadata.getJavaType();
            if (columnClass == null) {
                if (DATABASE_INTERVAL_NATIVE_TYPE.equals(columnMetadata.getNativeTypeMetadata())) {
                    // Handle Database Intervals as Java String type
                    columnClass = String.class;
                } else {
                    throw new DatabaseException("Type for columnName " + columnName + " is null");
                }
            }
            value = row.get(columnName, columnClass);

            if (fieldType.isEnum() && value instanceof String) {
                value = parseEnum(fieldType, (String) value);
            }

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
                            currentMap = (Map<String, Object>) currentValue;
                        } else {
                            throw new IllegalStateException("Key: " + key + " in columnName: " + columnName
                                    + " is already set to a value of class " + currentValue.getClass().getSimpleName()
                                    + ", but expected to be a Map. Is the value for the key set twice?");
                        }
                    }
                    else {
                        currentMap = new HashMap<>();
                        lastMap.put(key, currentMap);
                    }

                    lastMap = currentMap;
                }
            }

            lastMap.put(ReflectUtility.transformFromFieldNameToJsonName(modelField), value);
        }

        return objectMap;
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
