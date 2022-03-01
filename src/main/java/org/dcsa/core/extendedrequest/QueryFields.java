package org.dcsa.core.extendedrequest;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.sql.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class QueryFields {

    public static QueryField queryFieldFromFieldWithSelectPrefix(Field modelField, Table table, boolean selectable, String selectNamePrefix) {
        return queryFieldFromFieldWithSelectPrefix(modelField, table, selectable, selectNamePrefix, null);
    }

    public static QueryField queryFieldFromFieldWithSelectPrefix(Field modelField, Table table, boolean selectable, String selectNamePrefix, QueryFieldConditionGenerator conditionGenerator) {
        String tableAlias = ReflectUtility.getAliasId(table);
        String columnName;
        Column internalColumn;
        Column selectColumn = null;
        String prefixedJsonName = selectNamePrefix + ReflectUtility.transformFromFieldNameToJsonName(modelField);
        columnName = ReflectUtility.transformFromFieldToColumnName(modelField);
        internalColumn = table.column(SqlIdentifier.unquoted(columnName));
        if (selectable) {
            selectColumn = internalColumn.as(SqlIdentifier.quoted(prefixedJsonName));
        }
        return FieldBackedQueryField.of(
                modelField,
                internalColumn,
                selectColumn,
                tableAlias,
                prefixedJsonName,
                conditionGenerator
        );
    }

    public static QueryField nonSelectableQueryField(Column column, String jsonName, Class<?> type, QueryFieldConditionGenerator conditionGenerator) {
        Table table = Objects.requireNonNull(column.getTable(), "column.getTable() must be non-null");
        String joinAlias = ReflectUtility.getAliasId(table);
        if (column instanceof Aliased) {
            // Future proofing.
            throw new IllegalArgumentException("Column must not be aliased");
        }
        return SimpleQueryField.of(column, joinAlias, jsonName, type, conditionGenerator);
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

        private final QueryFieldConditionGenerator conditionGenerator;

        public Column getSelectColumn() {
            return null;
        }

        @Override
        public FilterCondition generateCondition(ComparisonType comparisonType, String fieldAttribute, List<String> queryParamValues, Function<String, Expression> value2BindVariable) {
            if (conditionGenerator == null) {
                return null;
            }
            return conditionGenerator.generateCondition(this, comparisonType, fieldAttribute, queryParamValues, value2BindVariable);
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

        private final QueryFieldConditionGenerator conditionGenerator;

        public Class<?> getType() {
            return combinedModelField.getType();
        }

        @Getter(lazy = true)
        @EqualsAndHashCode.Exclude
        private final String datePattern = ReflectUtility.getDateFormat(combinedModelField, null);

        @Override
        public FilterCondition generateCondition(ComparisonType comparisonType, String fieldAttribute, List<String> queryParamValues, Function<String, Expression> value2BindVariable) {
            if (conditionGenerator == null) {
                return null;
            }
            return conditionGenerator.generateCondition(this, comparisonType, fieldAttribute, queryParamValues, value2BindVariable);
        }
    }
}
