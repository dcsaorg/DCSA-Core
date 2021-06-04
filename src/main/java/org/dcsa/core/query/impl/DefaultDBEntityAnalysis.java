package org.dcsa.core.query.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.extendedrequest.TableAndJoins;
import org.dcsa.core.query.DBEntityAnalysis;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class DefaultDBEntityAnalysis<T> implements DBEntityAnalysis<T> {

    @Getter
    private final Class<T> entityType;
    private final Map<String, QueryField> jsonName2QueryField;
    private final Map<String, QueryField> javaFieldName2QueryField;
    private final Map<String, QueryField> selectName2QueryField;
    private final Set<String> declaredButNotSelectable;
    @Getter
    private final List<QueryField> allSelectableFields;
    @Getter

    private final TableAndJoins tableAndJoins;

    private static QueryField getFieldFromTable(Map<String, QueryField> table, String key) throws IllegalArgumentException {
        QueryField field = table.get(key);
        if (field == null) {
            throw new IllegalArgumentException(key);
        }
        return field;
    }

    public QueryField getQueryFieldFromJSONName(String jsonName) throws IllegalArgumentException {
        return getFieldFromTable(jsonName2QueryField, jsonName);
    }

    @Override
    public QueryField getQueryFieldFromJavaFieldName(String javaFieldName) throws IllegalArgumentException {
        return getFieldFromTable(javaFieldName2QueryField, javaFieldName);
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
