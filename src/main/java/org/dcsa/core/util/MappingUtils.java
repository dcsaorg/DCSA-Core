package org.dcsa.core.util;

import lombok.SneakyThrows;
import org.springframework.data.annotation.Transient;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class MappingUtils {

    /* For use with ".buffer(...).concatMap(service::createOrUpdateOrDeleteAll), etc. where the underlying
     * operation uses a variant "WHERE foo IN (LIST)".
     *
     * A higher number means fewer queries but after a certain size postgres performance will degrade.
     * Plus a higher number will also require more memory (java-side) as we build up a list of items.
     *
     * The number should be sufficient to bundle most trivial things into a single query without hitting
     * performance issues.
     */
    public static final int SQL_LIST_BUFFER_SIZE = 70;

    public static <C, S extends C, T extends C> T instanceFrom(S source, Supplier<T> targetConstructor, Class<C> clazz) {
        T target = targetConstructor.get();
        copyFields(source, target, clazz);
        return target;
    }

    @SneakyThrows({InvocationTargetException.class, IllegalAccessException.class})
    public static <C, S extends C, T extends C> void copyFields(S source, T target, Class<C> clazz) {
        /* Only clone the abstract fields that we know they share */
        Class<?> currentClass = clazz;
        Set<String> seenFields = new HashSet<>();
        while (currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                String fieldName = field.getName();
                Object value;
                /* skip fields that have already been seen in a subclass */
                if (!seenFields.add(fieldName)) {
                    continue;
                }
                if (field.isAnnotationPresent(Transient.class)) {
                    continue;
                }
                value = getFieldValue(source, field, currentClass);
                try {
                    field.set(target, value);
                } catch (IllegalAccessException e) {
                    ReflectUtility.setValueUsingMethod(target, currentClass, fieldName, field.getType(), value);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
    }

    private static Object getFieldValue(Object source, Field field, Class<?> declaringClass) throws InvocationTargetException, IllegalAccessException {
        try {
            return field.get(source);
        } catch (IllegalAccessException e) {
            Method getterMethod = getGetMethod(declaringClass, field.getName());
            return getterMethod.invoke(source);
        }
    }

    private static Method getGetMethod(Class<?> clazz, String fieldName) {
        String capitalizedFieldName = ReflectUtility.capitalize(fieldName);
        return ReflectUtility.getMethod(clazz, "get" + capitalizedFieldName);
    }
}
