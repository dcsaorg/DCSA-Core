package org.dcsa.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.relational.core.mapping.Column;

import javax.el.MethodNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtility {

    private ReflectUtility() { }

    /**
     * Tries to set a value on an object. First it tries to use the Column annotation on the field. If there are no
     * matches, then it tries to use the field name directly. If the field is not public it tries to find a setter
     * method to use instead.
     * @param obj the object to set the value on
     * @param columnName the name of the database column containing the value
     * @param objClass the type of the value to set
     * @param objectValue the value
     * @throws IllegalAccessException if the setter method is not public
     * @throws InvocationTargetException if the underlying method throws an exception
     */
    public static void setValue(Object obj, String columnName, Class<?> objClass, Object objectValue) throws IllegalAccessException, InvocationTargetException {
        Field[] fields = obj.getClass().getDeclaredFields();
        // Test for @Column annotation to match columnName of Row
        for (Field field: fields) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && columnName.equals(column.value())) {
                try {
                    field.set(obj, objectValue);
                } catch (IllegalAccessException illegalAccessException) {
                    setValueUsingMethod(obj, field.getName(), objClass, objectValue);
                }
                return;
            }
        }
        // Test for @Column annotation to match field name
        for (Field field: fields) {
            if (columnName.equals(field.getName())) {
                try {
                    field.set(obj, objectValue);
                } catch (IllegalAccessException illegalAccessException) {
                    setValueUsingMethod(obj, field.getName(), objClass, objectValue);
                }
                return;
            }
        }
        // Error if here
        throw new IllegalAccessException("Neither the field or a @Column annotation has been found matching the name: " + columnName + " on " + obj.getClass().getSimpleName());
    }

    /**
     * Sets a value on an object using setter methods instead of fieldName.
     * Throws RunTimeException if
     * @param obj the object on which to set a value
     * @param fieldName the name of the field to find a setter method in order to set a value
     * @param objClass the type of the object to set
     * @param objectValue the value to set
     */
    public static void setValueUsingMethod(Object obj, String fieldName, Class<?> objClass, Object objectValue) throws IllegalAccessException, InvocationTargetException {
        Method method = getSetterMethodFromName(obj.getClass(), fieldName, objClass);
        method.invoke(obj, objectValue);
    }

    /**
     * Finds the setter method on clazz corresponding to the name fieldName with the arguments valueFieldTypes
     * @param clazz the class to investigate
     * @param fieldName the name of the field to find a setter method for
     * @param valueFieldTypes the arguments for the setter method
     * @return the method corresponding to fieldName with valueFieldTypes as arguments
     */
    public static Method getSetterMethodFromName(Class<?> clazz, String fieldName, Class<?>... valueFieldTypes) {
        try {
            // Try the raw field name as a method call
            return getMethod(clazz, fieldName, valueFieldTypes);
        } catch (Exception exception) {
            String capitalizedFieldName = capitalize(fieldName);
            return getMethod(clazz, "set" + capitalizedFieldName, valueFieldTypes);
        }
    }

    /**
     * Investigate if a class contains a method be the name methodName with arguments corresponding to valueFieldTypes.
     * The method must be public.
     * Throws a MethodNotFoundException if the method requested is not public or does not exist
     * @param clazz the class to invistigate
     * @param methodName name of the method to return
     * @param valueFieldTypes arguments for the method to return
     * @return the public method corresponding to methodName and with the arguments containing valueFieldTypes
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... valueFieldTypes) {
        try {
            Method tempMethod = clazz.getMethod(methodName, valueFieldTypes);
            if ((tempMethod.getModifiers() & java.lang.reflect.Modifier.PUBLIC) == java.lang.reflect.Modifier.PUBLIC) {
                return tempMethod;
            } else {
                throw new MethodNotFoundException("Method: " + methodName + " is not public and thus cannot be accessed on Object:" + clazz.getName());
            }
        } catch (NoSuchMethodException noSuchMethodException) {
            throw new MethodNotFoundException("Method: " + methodName + " does not exist on on Object:" + clazz.getName(), noSuchMethodException);
        }
    }

    /**
     * Changes the first letter of name to uppercase and returns the result.
     *
     * @param name the name to change to Title-case
     * @return name with first letter capitalized
     */
    public static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    /**
     * Transforms a field name to a column name that can be used by a database query. If no @Column annotation exists
     * then fieldName is used
     *
     * @param clazz the Class
     * @param fieldName the name to convert
     * @return the database column name corresponding to fieldName
     * @throws NoSuchFieldException if fieldName does not exist on clazz
     */
    public static String transformFromFieldNameToColumnName(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        Column column = field.getDeclaredAnnotation(Column.class);
        if (column != null) {
            return column.value();
        } else {
            return fieldName;
        }
    }

    /**
     * Transforms a field name to a JSON name that can be used as a query parameter. If no @JsonProperty annotation exists
     * then fieldName is used
     *
     * @param clazz the Class
     * @param fieldName the name to convert
     * @return the JSON property name corresponding to fieldName
     * @throws NoSuchFieldException if fieldName does not exist on clazz
     */
    public static String transformFromFieldNameToJsonName(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(fieldName);
        JsonProperty jsonName = field.getDeclaredAnnotation(JsonProperty.class);
        if (jsonName != null) {
            return jsonName.value();
        } else {
            return fieldName;
        }
    }

    /**
     * Transform a name from the jsonName (used when serializing an object) to a model property. This method looks
     * through all fields names and checks if there is an annotation called JsonProperty. If it has such an annotation
     * it checks if the name corresponds to jsonName - if it does it will return the corresponding property name.
     * If all field names have been checked for a JsonProperty corresponding to the name and no matches have been found
     * it checks if there is an exact match with a property name. If so - the property name is returned.
     * This method throws a NoSuchFieldException if the field does not exist
     * @param clazz the class to investigate
     * @param jsonName the json name to transform into a property name
     * @return the field name corresponding to the jsonName
     * @throws NoSuchFieldException if no JsonProperty or fieldName corresponds to jsonName
     */
    public static String transformFromJsonNameToFieldName(Class<?> clazz, String jsonName) throws NoSuchFieldException {
        String fieldName = null;
        for (Field field: clazz.getDeclaredFields()) {
            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            if (jsonProperty != null) {
                if (jsonProperty.value().equals(jsonName)) {
                    return field.getName();
                }
            } else if (field.getName().equals(jsonName)) {
                fieldName = field.getName();
            }
        }
        if (fieldName != null) {
            return fieldName;
        } else {
            throw new NoSuchFieldException("Field " + jsonName + " is neither specified as a JsonProperty nor is it a field on " + clazz.getSimpleName());
        }
    }
}
