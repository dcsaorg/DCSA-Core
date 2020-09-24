package org.dcsa.core.extendedrequest;

import lombok.Getter;

@Getter
public class FilterItem {
    private final String fieldName;
    private final Class<?> clazz;
    private final String fieldValue;
    private final boolean exactMatch;
    private final boolean orGroup;
    private final boolean stringValue;

    public FilterItem(String fieldName, Class<?> clazz, String fieldValue, boolean exactMatch, boolean orGroup, boolean stringValue) {
        this.fieldName = fieldName;
        this.clazz = clazz;
        this.fieldValue = fieldValue;
        this.exactMatch = exactMatch;
        this.orGroup = orGroup;
        this.stringValue = stringValue;
    }

    public static FilterItem addStringFilter(String fieldName, Class<?> clazz, String value) {
        return new FilterItem(fieldName, clazz, value, false, false, true);
    }

    public static FilterItem addExactFilter(String fieldName, Class<?> clazz, String value, boolean stringValue) {
        return new FilterItem(fieldName, clazz, value, true, false, stringValue);
    }

    public static FilterItem addOrGroupFilter(String fieldName, Class<?> clazz, String value, boolean exactMatch) {
        return new FilterItem(fieldName, clazz, value, exactMatch, true, true);
    }
}
