package org.dcsa.util;

import java.util.ArrayList;
import java.util.List;

/**
 * A class used to store which filters to use when extracting Collection results. Depending on what filter to create, some
 * metadata is stored together with the filter
 */
public class Filter {
    List<String> fieldNames = new ArrayList<>();
    List<Class> clazzes = new ArrayList<>();
    List<String> fieldValues = new ArrayList<>();
    List<Boolean> exactMatch = new ArrayList<>();
    List<Boolean> enumMatch = new ArrayList<>();
    List<Boolean> stringValue = new ArrayList<>();

    public void addStringFilter(String field, String value) {
        addFilter(field, null, value, false, false, true);
    }

    public void addExactFilter(String field, String value, boolean stringValue) {
        addFilter(field, null, value, true, false, stringValue);
    }

    public void addExactFilter(String field, Class<?> clazz, String value, boolean stringValue) {
        addFilter(field, clazz, value, true, false, stringValue);
    }

    public void addEnumFilter(String field, String value) {
        addFilter(field, null, value, true, true, true);
    }

    private void addFilter(String fieldName, Class<?> clazz, String fieldValue, boolean exactMatch, boolean enumMatch, boolean stringValue) {
        this.fieldNames.add(fieldName);
        this.clazzes.add(clazz);
        this.fieldValues.add(fieldValue);
        this.exactMatch.add(exactMatch);
        this.enumMatch.add(enumMatch);
        this.stringValue.add(stringValue);
    }

    public String getFieldName(int index) {
        return fieldNames.get(index);
    }

    public Class<?> getClazz(int index) {
        return clazzes.get(index);
    }

    public String getFieldValue(int index) {
        return fieldValues.get(index);
    }

    public Boolean getExactMatch(int index) {
        return exactMatch.get(index);
    }

    public Boolean getEnumMatch(int index) {
        return enumMatch.get(index);
    }

    public Boolean getStringValue(int index) {
        return stringValue.get(index);
    }

    public int getFilterSize() {
        return fieldNames.size();
    }
}
