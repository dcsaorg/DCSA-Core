package org.dcsa.core.extendedrequest;

import lombok.Getter;

@Getter
public class FilterItem {
    private final String fieldName;
    private final Class<?> clazz;
    private final Object fieldValue;
    private final boolean exactMatch;
    private final boolean orGroup;
    private final boolean stringValue;
    private final boolean ignoreCase;
    private final int bindNumber;

    public FilterItem(String fieldName, Class<?> clazz, Object fieldValue, boolean exactMatch, boolean orGroup, boolean stringValue, boolean ignoreCase, int bindNumber) {
        this.fieldName = fieldName;
        this.clazz = clazz;
        this.fieldValue = fieldValue;
        this.exactMatch = exactMatch;
        this.orGroup = orGroup;
        this.stringValue = stringValue;
        this.ignoreCase = ignoreCase;
        this.bindNumber = bindNumber;
    }

    public String getBindName() {
        return fieldName + bindNumber;
    }
    public Object getQueryFieldValue() {
        if (exactMatch) {
            return fieldValue;
        } else {
            return "%" + fieldValue + "%";
        }
    }
    public boolean doBind() {
        return true;
    }
    public static FilterItem addStringFilter(String fieldName, Class<?> clazz, String value, boolean ignoreCase, int bindNumber) {
        return new FilterItem(fieldName, clazz, value, false, false, true, ignoreCase, bindNumber);
    }

    public static FilterItem addExactFilter(String fieldName, Class<?> clazz, Object value, boolean stringValue, int bindNumber) {
        return new FilterItem(fieldName, clazz, value, true, false, stringValue, false, bindNumber);
    }

    public static FilterItem addOrGroupFilter(String fieldName, Class<?> clazz, String value, boolean exactMatch, int bindNumber) {
        return new FilterItem(fieldName, clazz, value, exactMatch, true, true, false, bindNumber);
    }
}
