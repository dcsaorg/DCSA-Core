package org.dcsa.core.util;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dcsa.core.exception.GetException;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.sql.Aliased;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.data.relational.core.sql.SqlIdentifier;

import javax.el.MethodNotFoundException;
import javax.validation.constraints.NotNull;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A helper class with a lot of Reflection utilities
 */
public class ReflectUtility {

    private ReflectUtility() { }

    /**
     * Tries to set a value on an object. First it tries to use the Column annotation on the field. If there are no
     * matches, then it tries to use the field name directly. If the field is not public it tries to find a setter
     * method to use instead.
     * @param obj the object to set the value on
     * @param javaFieldName the name of the java field for the value
     * @param objClass the type of the value to set
     * @param objectValue the value
     * @throws IllegalAccessException if the setter method is not public
     * @throws InvocationTargetException if the underlying method throws an exception
     */
    public static void setValue(Object obj, String javaFieldName, Class<?> objClass, Object objectValue) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        setValue(obj, obj.getClass(), javaFieldName, objClass, objectValue);
    }

    /**
     * Tries to set a value on an object using the specific class of the object. First it tries to use the Column annotation on the field. If there are no
     * matches, then it tries to use the field name directly. If the field is not public it tries to find a setter
     * method to use instead.
     * @param obj the object to set the value on
     * @param clazz the class of the object to set the value on
     * @param javaFieldName the name of the field for the value
     * @param valueClass the type of the value to set
     * @param value the value
     * @throws IllegalAccessException if the setter method is not public
     * @throws InvocationTargetException if the underlying method throws an exception
     */
    private static void setValue(Object obj, Class<?> clazz, String javaFieldName, Class<?> valueClass, Object value) throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        Field field = getDeclaredField(clazz, javaFieldName);
        try {
            field.set(obj, value);
        } catch (IllegalAccessException illegalAccessException) {
            setValueUsingMethod(obj, clazz, field.getName(), valueClass, value);
        }
    }

    /**
     * Sets a value on an object using setter methods instead of fieldName.
     * Throws RunTimeException if
     * @param obj the object on which to set a value
     * @param clazz the class of the object on which to set a value
     * @param fieldName the name of the field to find a setter method in order to set a value
     * @param objClass the type of the object to set
     * @param objectValue the value to set
     */
    public static void setValueUsingMethod(Object obj, Class<?> clazz, String fieldName, Class<?> objClass, Object objectValue) throws IllegalAccessException, InvocationTargetException {
        Method method = getSetterMethodFromName(clazz, fieldName, objClass);
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
        return getAccessorFromName(clazz, "set", fieldName, valueFieldTypes);
    }

    /**
     * Finds the getter method on clazz corresponding to the name fieldName with the arguments valueFieldTypes
     * @param clazz the class to investigate
     * @param fieldName the name of the field to find a getter method for
     * @return the method corresponding to fieldName with valueFieldTypes as arguments
     */
    public static Method getGetterMethodFromName(Class<?> clazz, String fieldName) {
        return getAccessorFromName(clazz, "get", fieldName);
    }

    private static Method getAccessorFromName(Class<?> clazz, String prefix, String fieldName, Class<?> ... valueFieldTypes) {
        try {
            // Try the raw field name as a method call
            return getMethod(clazz, fieldName, valueFieldTypes);
        } catch (Exception exception) {
            String capitalizedFieldName = capitalize(fieldName);
            return getMethod(clazz, prefix + capitalizedFieldName, valueFieldTypes);
        }
    }

    /**
     * Investigate if a class contains a method be the name methodName with arguments corresponding to valueFieldTypes.
     * The method must be public.
     * Throws a MethodNotFoundException if the method requested is not public or does not exist
     * @param clazz the class to investigate
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
     * then fieldName is used. If no match on clazz it will try the super class until Object.class is reached
     *
     * @param clazz the Class
     * @param fieldName the name to convert
     * @return the database column name corresponding to fieldName
     * @throws NoSuchFieldException if fieldName does not exist on clazz
     */
    public static String transformFromFieldNameToColumnName(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = getDeclaredField(clazz, fieldName);
        return transformFromFieldToColumnName(field);
    }

    /**
     * Transforms a field to a column name that can be used by a database query. If no @Column annotation exists
     * then the field name is used.
     *
     * @return the database column name corresponding to fieldName
     */
    public static String transformFromFieldToColumnName(Field field) {
        Column column = field.getDeclaredAnnotation(Column.class);
        if (column != null) {
            return column.value();
        } else {
            return field.getName();
        }
    }

    /**
     * Transforms a field to a JSON name that can be used as a query parameter. If no @JsonProperty annotation exists
     * then field name is used.
     *
     * @param field the Field
     * @return the JSON property name corresponding to fieldName
     */
    public static String transformFromFieldNameToJsonName(Field field) {
        JsonProperty jsonName = field.getDeclaredAnnotation(JsonProperty.class);
        if (jsonName != null && !jsonName.value().equals(JsonProperty.USE_DEFAULT_NAME)) {
            return jsonName.value();
        } else {
            return field.getName();
        }
    }

    /**
     * Transforms a field name to a JSON name that can be used as a query parameter. If no @JsonProperty annotation exists
     * then fieldName is used. If no match on clazz it will try the super class until Object.class is reached
     *
     * @param clazz the Class
     * @param fieldName the name to convert
     * @return the JSON property name corresponding to fieldName
     * @throws NoSuchFieldException if fieldName does not exist on clazz
     */
    public static String transformFromFieldNameToJsonName(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Field field = getDeclaredField(clazz, fieldName);
        return transformFromFieldNameToJsonName(field);
    }

    /**
     * Transform a name from the jsonName (used when serializing an object) to a model property. This method looks
     * through all fields names and checks if there is an annotation called JsonProperty. If it has such an annotation
     * it checks if the name corresponds to jsonName - if it does it will return the corresponding property name.
     * If all field names have been checked for a JsonProperty corresponding to the name and no matches have been found
     * it checks if there is an exact match with a property name. If so - the property name is returned.
     * If there is no match in clazz it will continue with the super class until the Object.class is reached
     * This method throws a NoSuchFieldException if the field does not exist
     * @param clazz the class to investigate
     * @param jsonName the json name to transform into a property name
     * @return the field name corresponding to the jsonName
     * @throws NoSuchFieldException if no JsonProperty or fieldName corresponds to jsonName
     */
    public static String transformFromJsonNameToFieldName(Class<?> clazz, String jsonName) throws NoSuchFieldException {
        String fieldName = null;
        for (Field field: clazz.getDeclaredFields()) {
            if (field.isSynthetic()) {
                continue;
            }
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
            // Try the super class
            clazz = clazz.getSuperclass();
            if (clazz != Object.class) {
                return transformFromJsonNameToFieldName(clazz, jsonName);
            }
            // No matching fieldName - throw an exception
            throw new NoSuchFieldException("Field " + jsonName + " is neither specified as a JsonProperty nor is it a field on " + clazz.getSimpleName());
        }
    }

    /**
     * A method to concatenate all field names corresponding to the type *type* on class clazz.
     * @param clazz the class to investigate
     * @param type the type to look for
     * @return the list of fieldNames with type *type* on class clazz
     */
    public static String[] getFieldNamesOfType(Class<?> clazz, Class<?> type) {
        List<String> fieldNames = new ArrayList<>();
        getFieldNamesOfType(clazz, type, fieldNames);
        return fieldNames.toArray(new String[0]);
    }

    /**
     * A helper method to concatenate all field names corresponding to the type *type* on class clazz.
     * If there is no match in clazz it will continue with the super class until the Object.class is reached
     * @param clazz the class to investigate
     * @param type the type to look for
     * @param fieldNames the list of fieldNames with type *type* on class clazz
     */
    private static void getFieldNamesOfType(Class<?> clazz, Class<?> type, List<String> fieldNames) {
        for (Field field: clazz.getDeclaredFields()) {
            if (!field.isSynthetic() && field.getType() == type) {
                fieldNames.add(field.getName());
            }
        }

        // Try the super class
        clazz = clazz.getSuperclass();
        if (clazz != Object.class) {
            getFieldNamesOfType(clazz, type, fieldNames);
        }
    }

    /**
     * Gets the date (and time) format for a date value. Looks at the JSonFormat - if that cannot be found it uses the
     * default value provided
     *
     * @param field the field to retrieve the dateFormat from
     * @param defaultValue the default value to use if no value is set on the field
     * @return the date format to use
     */
    public static String getDateFormat(Field field, String defaultValue) {
        JsonFormat jsonFormat = field.getAnnotation(JsonFormat.class);
        if (jsonFormat != null) {
            return jsonFormat.pattern();
        } else {
            return defaultValue;
        }
    }

    public static @NotNull Table getTable(@NotNull Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new IllegalStateException("@Table not defined on " + clazz.getSimpleName() + "-class!");
        } else {
            return table;
        }
    }

    /**
     * A helper method to get a field by the name of fieldName on the class provided by clazz
     *
     * @param clazz the class to investigate
     * @param fieldName the name of the field to find
     * @return the field with the name fieldName on the class clazz
     * @throws NoSuchFieldException if no field of the name fieldName exists on the class clazz
     */
    public static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(fieldName);
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException noSuchFieldException) {
            // Try the super class
            clazz = clazz.getSuperclass();
            if (clazz != Object.class) {
                return getDeclaredField(clazz, fieldName);
            }
            throw noSuchFieldException;
        }
    }

    public static void visitAllFields(Class<?> clazz, Predicate<Field> matching, Consumer<Field> fieldConsumer) {
        visitAllFields(clazz, matching, fieldConsumer, new HashSet<>());
    }

    private static void visitAllFields(Class<?> clazz, Predicate<Field> matching, Consumer<Field> fieldConsumer, Set<String> seenFields) {
        Class<?> currentClass = clazz;
        if (matching == null) {
            matching = (f) -> true;
        }
        while (currentClass != Object.class) {
            for (Field f : currentClass.getDeclaredFields()) {
                if (!f.isSynthetic() && !seenFields.contains(f.getName()) && matching.test(f)) {
                    seenFields.add(f.getName());
                    fieldConsumer.accept(f);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    public static <T> Class<T> getConcreteModelClassForService(Class<?> implClass) {
        Type genericSuperclass = implClass.getGenericSuperclass();
        if (!(genericSuperclass instanceof ParameterizedType)) {
            throw new IllegalStateException("Cannot automatically determine model class in " + implClass.getSimpleName());
        }
        ParameterizedType parameterizedType = (ParameterizedType)genericSuperclass;
        Type[] types = parameterizedType.getActualTypeArguments();
        Class<?> typeVar = null;

        for (Type t : types) {
            if (!(t instanceof Class)) {
                continue;
            }
            Class<?> tVar = (Class<?>) t;
            if (! tVar.isAnnotationPresent(Table.class)) {
                continue;
            }
            if (typeVar != null) {
                throw new IllegalStateException("Cannot automatically determine model class in " + implClass.getSimpleName()
                        + ": Both " + tVar.getSuperclass() + " and " + typeVar.getSimpleName() + " had a @Table annotation");
            }
            typeVar = tVar;
        }
        if (typeVar == null) {
            throw new IllegalStateException("Cannot automatically determine model class in " + implClass.getSimpleName()
                    + ": None of Type Variables had a @Table annotation");
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        Class<T> modelClass = (Class)typeVar;
        return modelClass;
    }

    public static String getTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            throw new GetException("@Table not defined on class: " + clazz.getSimpleName());
        }
        return table.value();
    }


    public static String getAliasId(org.springframework.data.relational.core.sql.Table table) {
        SqlIdentifier aliasId;
        if (table instanceof Aliased) {
            aliasId = ((Aliased) table).getAlias();
        } else {
            aliasId = table.getReferenceName();
        }
        return aliasId.getReference(IdentifierProcessing.NONE);
    }
}
