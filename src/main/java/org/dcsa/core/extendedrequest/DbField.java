package org.dcsa.core.extendedrequest;

import lombok.*;
import org.dcsa.core.model.ModelClass;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.mapping.Column;

import java.lang.reflect.Field;

@RequiredArgsConstructor(staticName = "of")
@Getter
@EqualsAndHashCode
class DbField implements QueryField {

    @NonNull
    private final Class<?> combinedModelClass;

    @NonNull
    private final Field combinedModelField;

    @NonNull
    private final Class<?> originalModelClass;

    @NonNull
    private final String tableJoinAlias;

    private final boolean selectable;


    /* lombok generated cached fields - we exclude them from equals + hashCode as an optimization (they
     * are derived from the the above fields, so they do not provide anything unique).
     */
    @Getter(lazy = true)
    @EqualsAndHashCode.Exclude
    private final String originalTableName = ReflectUtility.getTable(this.getOriginalModelClass()).value();

    @Getter(lazy = true)
    @EqualsAndHashCode.Exclude
    private final String originalColumnName = computeOriginalColumnName();

    @Getter(lazy = true)
    @EqualsAndHashCode.Exclude
    private final String selectColumnName = computeSelectColumnName();

    @Getter(lazy = true)
    @EqualsAndHashCode.Exclude
    private final String jsonName = ReflectUtility.transformFromFieldNameToJsonName(combinedModelField);

    @Getter(lazy = true)
    @EqualsAndHashCode.Exclude
    private final String datePattern = ReflectUtility.getDateFormat(combinedModelField, null);

    public String getQueryInternalName() {
        return this.tableJoinAlias + "." + this.getOriginalColumnName();
    }

    public Class<?> getType() {
        return combinedModelField.getType();
    }

    @SneakyThrows(NoSuchFieldException.class)
    private String computeOriginalColumnName() {
        assert combinedModelField != null;
        Class<?> modelClass = combinedModelClass;
        if (!combinedModelField.isAnnotationPresent(ModelClass.class)) {
            /* Special-case: Use the primary model when the field does not have a ModelClass */
            modelClass = originalModelClass;
        }
        return ReflectUtility.transformFromFieldNameToColumnName(modelClass, combinedModelField.getName());
    }

    private String computeSelectColumnName() {
        assert combinedModelField != null;
        if (!selectable) {
            return null;
        }
        Column column = combinedModelField.getDeclaredAnnotation(Column.class);
        if (column != null) {
            // Use that name as requested
            return column.value();
        }
        /* Use the Field name.  By definition we can only have one of it anyway and the database does not mind. */
        return combinedModelField.getName();
    }
}
