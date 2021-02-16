package org.dcsa.core.extendedrequest;

import lombok.Getter;

@Getter
public class FilterItem {
    private final QueryField queryField;
    private final Object fieldValue;
    private final boolean exactMatch;
    private final int orGroup;
    private final boolean stringValue;
    private final boolean ignoreCase;
    private final int bindNumber;
    private final String dateFormat;

    public FilterItem(QueryField queryField, String dateFormat, Object fieldValue, boolean exactMatch, int orGroup, boolean stringValue, boolean ignoreCase, int bindNumber) {
        this.queryField = queryField;
        this.fieldValue = fieldValue;
        this.exactMatch = exactMatch;
        this.orGroup = orGroup;
        this.stringValue = stringValue;
        this.ignoreCase = ignoreCase;
        this.bindNumber = bindNumber;
        this.dateFormat = dateFormat;
    }

    public String getBindName() {
        return this.queryField.getJsonName() + bindNumber;
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

    public static FilterItem addNumberFilter(QueryField queryField, String value, int bindNumber) {
        return new FilterItem(queryField, null, value, false, 0, false, false, bindNumber);
    }

    public static FilterItem addStringFilter(QueryField queryField, String value, boolean ignoreCase, int bindNumber) {
        return new FilterItem(queryField, null, value, false, 0, true, ignoreCase, bindNumber);
    }

    public static FilterItem addExactFilter(QueryField queryField, Object value, boolean stringValue, int bindNumber) {
        return new FilterItem(queryField, null, value, true, 0, stringValue, false, bindNumber);
    }

    public static FilterItem addDateFilter(QueryField queryField, String dateFormat, Object value, boolean stringValue, int bindNumber) {
        return new FilterItem(queryField, dateFormat, value, false, 0, stringValue, true, bindNumber);
    }

    public static FilterItem addOrGroupFilter(QueryField queryField, String value, boolean exactMatch, int bindNumber, int orGroup) {
        return new FilterItem(queryField, null, value, exactMatch, orGroup, true, !exactMatch, bindNumber);
    }
}
