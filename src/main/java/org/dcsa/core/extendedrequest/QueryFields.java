package org.dcsa.core.extendedrequest;

import lombok.Data;
import lombok.NonNull;

import java.lang.reflect.Field;

public class QueryFields {

    public static QueryField queryFieldFromField(Class<?> modelType, Field field, Class<?> modelClassForField, String joinAlias, boolean selectable) {
        return DbField.of(modelType, field, modelClassForField, joinAlias, selectable);
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
