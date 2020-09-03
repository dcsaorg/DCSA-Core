package org.dcsa.base.util;

public class FilterItem {
    String fieldName;
    Class<?> clazz;
    String fieldValue;
    boolean exactMatch;
    boolean orGroup;
    boolean stringValue;

    public FilterItem(String fieldName, Class<?> clazz, String fieldValue, boolean exactMatch, boolean orGroup, boolean stringValue) {
        this.fieldName = fieldName;
        this.clazz = clazz;
        this.fieldValue = fieldValue;
        this.exactMatch = exactMatch;
        this.orGroup = orGroup;
        this.stringValue = stringValue;
    }
}
