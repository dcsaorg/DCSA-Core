package org.dcsa.core.extendedrequest;

import lombok.Data;
import lombok.NonNull;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.sql.Aliased;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Table;

import java.lang.reflect.Field;
import java.util.Objects;

public class QueryFields {

    public static QueryField queryFieldFromField(Class<?> modelType, Field field, Class<?> modelClassForField, String joinAlias, boolean selectable) {
        return DbField.of(modelType, field, modelClassForField, joinAlias, selectable);
    }

    public static QueryField nonSelectableQueryField(Column column, String jsonName, Class<?> type) {
        return nonSelectableQueryField(column, jsonName, type, null);
    }

    public static QueryField nonSelectableQueryField(Column column, String jsonName, Class<?> type, String dateFormat) {
        Table table = Objects.requireNonNull(column.getTable(), "column.getTable() must be non-null");
        String joinAlias = ReflectUtility.getAliasId(table);
        String columnName = column.getName().toString();
        if (column instanceof Aliased) {
            // Future proofing.
            throw new IllegalArgumentException("Column must not be aliased");
        }
        return SimpleQueryField.of(joinAlias, columnName, jsonName, type, dateFormat);
    }

    public static QueryField nonSelectableQueryField(String joinAlias, String columnName, String jsonName, Class<?> type) {
        return nonSelectableQueryField(joinAlias, columnName, jsonName, type, null);
    }

    public static QueryField nonSelectableQueryField(String joinAlias, String columnName, String jsonName, Class<?> type, String dateFormat) {
        return SimpleQueryField.of(joinAlias, columnName, jsonName, type, dateFormat);
    }

    @Data(staticConstructor = "of")
    private static class SimpleQueryField implements QueryField {
        @NonNull
        private final String tableJoinAlias;

        @NonNull
        private final String originalColumnName;

        @NonNull
        private final String jsonName;

        private final Class<?> type;

        private final String datePattern;

        private String internalQueryName;

        public String getQueryInternalName() {
            if (internalQueryName == null) {
                internalQueryName = this.getTableJoinAlias() + "." + this.getOriginalColumnName();
            }
            return internalQueryName;
        }
    }
}
