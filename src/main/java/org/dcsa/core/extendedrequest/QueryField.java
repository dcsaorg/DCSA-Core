package org.dcsa.core.extendedrequest;

import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.OrderByField;

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
    default String getFieldPath() {
        return null;
    }
    default Field getCombinedModelField() {
        return null;
    }

    default OrderByField asOrderByField(Sort.Direction direction) {
        Column column = getSelectColumn();
        if (column == null) {
            column = getInternalQueryColumn();
        }
        return OrderByField.from(column, direction);
    }
}
