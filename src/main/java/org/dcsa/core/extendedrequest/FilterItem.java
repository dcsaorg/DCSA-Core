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
    private final String dateFormat;

    public FilterItem(String fieldName, String dateFormat, Class<?> clazz, Object fieldValue, boolean exactMatch, boolean orGroup, boolean stringValue, boolean ignoreCase, int bindNumber) {
        this.fieldName = fieldName;
        this.clazz = clazz;
        this.fieldValue = fieldValue;
        this.exactMatch = exactMatch;
        this.orGroup = orGroup;
        this.stringValue = stringValue;
        this.ignoreCase = ignoreCase;
        this.bindNumber = bindNumber;
        this.dateFormat = dateFormat;
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

    public static FilterItem addNumberFilter(String fieldName, Class<?> clazz, String value, int bindNumber) {
        return new FilterItem(fieldName, null, clazz, value, false, false, false, false, bindNumber);
    }

    public static FilterItem addStringFilter(String fieldName, Class<?> clazz, String value, boolean ignoreCase, int bindNumber) {
        return new FilterItem(fieldName, null, clazz, value, false, false, true, ignoreCase, bindNumber);
    }

    public static FilterItem addExactFilter(String fieldName, Class<?> clazz, Object value, boolean stringValue, int bindNumber) {
        return new FilterItem(fieldName, null, clazz, value, true, false, stringValue, false, bindNumber);
    }

    public static FilterItem addDateFilter(String fieldName, String dateFormat, Class<?> clazz, Object value, boolean stringValue, int bindNumber) {
        return new FilterItem(fieldName, dateFormat, clazz, value, false, false, stringValue, true, bindNumber);
    }

    public static FilterItem addOrGroupFilter(String fieldName, Class<?> clazz, String value, boolean exactMatch, int bindNumber) {
        return new FilterItem(fieldName, null, clazz, value, exactMatch, true, true, false, bindNumber);
    }
}
