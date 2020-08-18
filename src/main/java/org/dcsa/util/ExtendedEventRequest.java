package org.dcsa.util;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import org.dcsa.exception.GetException;
import org.dcsa.model.Event;
import org.springframework.data.relational.core.mapping.Table;

import javax.el.MethodNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ExtendedEventRequest extends ExtendedRequest<Event> {
    private Class<Event>[] modelSubClasses;

    public ExtendedEventRequest(ExtendedParameters extendedParameters, Class<Event>[] modelSubClasses) {
        super(extendedParameters, Event.class);
        this.modelSubClasses = modelSubClasses;
    }

    @Override
    String transformFromJsonNameToFieldName(String jsonName) throws NoSuchFieldException {
        // Run through all possible subClasses and see if one of them can transform the JSON name to a field name
        for (Class<Event> clazz : modelSubClasses) {
            try {
                // Verify that the field exists on the model class and transform it from JSON-name to FieldName
                return ReflectUtility.transformFromJsonNameToFieldName(clazz, jsonName);
            } catch (NoSuchFieldException noSuchFieldException) {
                // Do nothing - try the next sub class
            }
        }
        throw new NoSuchFieldException("Field: " + jsonName + " does not exist on any of: " + getModelClassNames());
    }

    @Override
    Class<?> getFilterParameterReturnType(String fieldName) throws MethodNotFoundException {
        // Run through all possible subClasses
        for (Class<Event> clazz : modelSubClasses) {
            try {
                // Investigate if the return type of the getter method corresponding to fieldName is an Enum
                return ReflectUtility.getReturnTypeFromGetterMethod(clazz, fieldName);
            } catch (MethodNotFoundException methodNotFoundException) {
                // Do nothing - try the next sub class
            }
        }
        throw new MethodNotFoundException("No getter method found for field: " + fieldName + " tested the following subclasses: " + getModelClassNames());
    }

    @Override
    public String getTableName() {
        Table table = Event.class.getAnnotation(Table.class);
        if (table == null) {
            throw new GetException("@Table not defined on Event-class!");
        }
        return table.value();
    }

    @Override
    public boolean ignoreUnknownProperties() {
        // Always ignore unknown properties when using Event class (the properties are on the sub classes
        return true;
    }

    @Override
    public Event getModelClassInstance(Row row, RowMetadata meta) {
        try {
            JsonSubTypes jsonSubTypes = Event.class.getAnnotation(JsonSubTypes.class);
            JsonTypeInfo jsonTypeInfo = Event.class.getAnnotation(JsonTypeInfo.class);
            if (jsonSubTypes != null && jsonTypeInfo != null) {
                String property = jsonTypeInfo.property();
                // The discriminator value is on the Event class
                String columnName = ReflectUtility.transformFromFieldNameToColumnName(Event.class, property);
                Object value = row.get(columnName);
                for (JsonSubTypes.Type type : jsonSubTypes.value()) {
                    if (type.name().equals(value)) {
                        // Create a new instance of the sub class to Event
                        Constructor<?> constructor = type.value().getDeclaredConstructor();
                        return (Event) constructor.newInstance();
                    }
                }
                throw new GetException("Unmatched sub-type: " + value + " of Event.class");
            } else {
                return super.getModelClassInstance(row, meta);
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            throw new GetException("Error when creating a new sub class of Event.class");
        }
    }

    @Override
    String transformFromFieldNameToColumnName(String fieldName) throws NoSuchFieldException {
        // Run through all possible subClasses and see if one of them can transform the fieldName name to a column name
        for (Class<Event> clazz : modelSubClasses) {
            try {
                // Verify that the field exists on the model class and transform it from JSON-name to FieldName
                return ReflectUtility.transformFromFieldNameToColumnName(clazz, fieldName);
            } catch (NoSuchFieldException noSuchFieldException) {
                // Do nothing - try the next sub class
            }
        }
        throw new NoSuchFieldException("Field: " + fieldName + " does not exist on any of: " + getModelClassNames());
    }

    @Override
    String transformFromFieldNameToJsonName(String fieldName) throws NoSuchFieldException {
        // Run through all possible subClasses and see if one of them can transform the fieldName name to a json name
        for (Class<Event> clazz : modelSubClasses) {
            try {
                // Verify that the field exists on the model class and transform it from FieldName to JSON-name
                return ReflectUtility.transformFromFieldNameToJsonName(clazz, fieldName);
            } catch (NoSuchFieldException noSuchFieldException) {
                // Do nothing - try the next sub class
            }
        }
        throw new NoSuchFieldException("Field: " + fieldName + " does not exist on any of: " + getModelClassNames());
    }

    private String getModelClassNames() {
        StringBuilder sb = new StringBuilder();
        for (Class<Event> clazz : modelSubClasses) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(clazz.getSimpleName());
        }
        return sb.toString();
    }
}
