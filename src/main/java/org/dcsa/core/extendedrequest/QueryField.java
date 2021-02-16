package org.dcsa.core.extendedrequest;

import java.lang.reflect.Field;

public interface QueryField {

    default String getSelectColumnName() {
        return null;
    }
    String getOriginalColumnName();
    String getJsonName();
    String getTableJoinAlias();
    Class<?> getType();

    default boolean isSelectable() {
        return getSelectColumnName() != null;
    }

    default String getQueryInternalName() {
        return this.getTableJoinAlias() + "." + this.getOriginalColumnName();
    }

    default String getDatePattern() {
        return null;
    }
    default Field getCombinedModelField() {
        return null;
    }
}
