package org.dcsa.core.extendedrequest;

import org.springframework.data.relational.core.sql.Column;

import java.lang.reflect.Field;

public interface QueryField {

    String getJsonName();
    String getTableJoinAlias();
    Column getSelectColumn();
    Column getInternalQueryColumn();
    Class<?> getType();

    default boolean isSelectable() {
        return getSelectColumn() != null;
    }

    default String getDatePattern() {
        return null;
    }
    default Field getCombinedModelField() {
        return null;
    }
}
