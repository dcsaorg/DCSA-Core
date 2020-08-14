package org.dcsa.util;

import java.util.ArrayList;
import java.util.List;

public class Filter {
    List<String> fieldNames = new ArrayList<>();
    List<String> fieldValues = new ArrayList<>();

    public void addFilter(String field, String value) {
        fieldNames.add(field);
        fieldValues.add(value);
    }

    public String getFieldName(int index) {
        return fieldNames.get(index);
    }

    public String getFieldValue(int index) {
        return fieldValues.get(index);
    }

    public int getFilterSize() {
        return fieldNames.size();
    }
}
