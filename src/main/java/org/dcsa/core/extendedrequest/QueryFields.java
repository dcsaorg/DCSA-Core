package org.dcsa.core.extendedrequest;

import lombok.*;
import org.dcsa.core.model.ModelClass;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.sql.Aliased;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.data.relational.core.sql.Table;

import java.lang.reflect.Field;
import java.util.Objects;

public class QueryFields {

    public static QueryField queryFieldFromField(Class<?> combinedModelClass, Field combinedModelField, Class<?> originalModelClass, Table table, boolean selectable) {
        return queryFieldFromFieldWithSelectPrefix(combinedModelClass, combinedModelField, originalModelClass, table, selectable, "");
    }

    @SneakyThrows(NoSuchFieldException.class)
    public static QueryField queryFieldFromFieldWithSelectPrefix(Class<?> combinedModelClass, Field combinedModelField, Class<?> originalModelClass, Table table, boolean selectable, String namePrefix) {
        Class<?> modelClass = combinedModelClass;
        String tableAlias = ReflectUtility.getAliasId(table);
        String columnName;
        Column internalColumn;
        Column selectColumn = null;
        String prefixedJsonName = namePrefix + ReflectUtility.transformFromFieldNameToJsonName(combinedModelField);
        System.out.println(prefixedJsonName);
        if (!combinedModelField.isAnnotationPresent(ModelClass.class)) {
            /* Special-case: Use the primary model when the field does not have a ModelClass */
            modelClass = originalModelClass;
        }
        columnName = ReflectUtility.transformFromFieldNameToColumnName(modelClass, combinedModelField.getName());
        internalColumn = table.column(SqlIdentifier.unquoted(columnName));
        if (selectable) {
            selectColumn = internalColumn.as(SqlIdentifier.quoted(prefixedJsonName));
        }
        return FieldBackedQueryField.of(
                combinedModelField,
                internalColumn,
                selectColumn,
                tableAlias,
                prefixedJsonName
        );
    }

    public static QueryField nonSelectableQueryField(Column column, String jsonName, Class<?> type) {
        Table table = Objects.requireNonNull(column.getTable(), "column.getTable() must be non-null");
        String joinAlias = ReflectUtility.getAliasId(table);
        if (column instanceof Aliased) {
            // Future proofing.
            throw new IllegalArgumentException("Column must not be aliased");
        }
        return SimpleQueryField.of(column, joinAlias, jsonName, type);
    }

    @Data(staticConstructor = "of")
    private static class SimpleQueryField implements QueryField {

        @NonNull
        private final Column internalQueryColumn;

        @NonNull
        private final String tableJoinAlias;

        @NonNull
        private final String jsonName;

        private final Class<?> type;

        public Column getSelectColumn() {
            return null;
        }
    }

    @Data(staticConstructor = "of")
    private static class FieldBackedQueryField implements QueryField {

        @NonNull
        @Getter
        private final Field combinedModelField;

        @NonNull
        private final Column internalQueryColumn;

        private final Column selectColumn;

        @NonNull
        private final String tableJoinAlias;

        @NonNull
        private final String jsonName;

        public Class<?> getType() {
            return combinedModelField.getType();
        }

        @Getter(lazy = true)
        @EqualsAndHashCode.Exclude
        private final String datePattern = ReflectUtility.getDateFormat(combinedModelField, null);
    }
}
