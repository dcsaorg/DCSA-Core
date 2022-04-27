package org.dcsa.core.query.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.extendedrequest.QueryFieldRestriction;
import org.dcsa.core.extendedrequest.TableAndJoins;
import org.dcsa.core.query.DBEntityAnalysis;

import java.util.*;

@RequiredArgsConstructor
public class DefaultDBEntityAnalysis<T> implements DBEntityAnalysis<T> {

    private final Map<String, QueryField> jsonName2QueryField;
    private final Map<String, QueryFieldRestriction> jsonName2QueryFieldRestriction;
    private final Map<String, QueryField> selectName2QueryField;
    private final Set<String> declaredButNotSelectable;
    @Getter
    private final List<QueryField> allSelectableFields;
    @Getter

    private final TableAndJoins tableAndJoins;

    public Collection<QueryField> getQueryFields() {
      return jsonName2QueryField.values();
    }

    private static <T> T getFieldFromTable(Map<String, T> table, String key) throws IllegalArgumentException {
        T field = table.get(key);
        if (field == null) {
            throw new IllegalArgumentException(key);
        }
        return field;
    }

    public QueryFieldRestriction getQueryFieldRestriction(QueryField queryField) throws IllegalArgumentException {
      return jsonName2QueryFieldRestriction.get(queryField.getJsonName());
    }


    public QueryField getQueryFieldFromJSONName(String jsonName) throws IllegalArgumentException {
        return getFieldFromTable(jsonName2QueryField, jsonName);
    }

    @Override
    public QueryField getQueryFieldFromJavaFieldName(String javaFieldName) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public QueryField getQueryFieldFromSelectName(String selectName) throws IllegalArgumentException {
        try {
            return getFieldFromTable(selectName2QueryField, selectName);
        } catch (IllegalArgumentException e) {
            if (declaredButNotSelectable.contains(selectName)) {
                throw new IllegalArgumentException("Invalid " + selectName + ": The field is marked as non-selectable");
            }
            throw e;
        }
    }
}
