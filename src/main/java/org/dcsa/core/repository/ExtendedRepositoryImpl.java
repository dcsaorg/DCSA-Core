package org.dcsa.core.repository;

import io.r2dbc.spi.ColumnMetadata;
import org.dcsa.core.exception.DatabaseException;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.extendedrequest.QueryField;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ExtendedRepositoryImpl<T, I> extends SimpleR2dbcRepository<T, I> implements ExtendedRepository<T, I> {

    private static final Integer DATABASE_INTERVAL_NATIVE_TYPE = 1186;

    private final DatabaseClient databaseClient;

    public ExtendedRepositoryImpl(MappingRelationalEntityInformation<T, I> mappingRelationalEntityInformation,
                                  R2dbcEntityTemplate r2dbcEntityTemplate,
                                  MappingR2dbcConverter mappingR2dbcConverter) {
        super(mappingRelationalEntityInformation, r2dbcEntityTemplate, mappingR2dbcConverter);
        this.databaseClient = r2dbcEntityTemplate.getDatabaseClient();
    }

    public Mono<Integer> countAllExtended(final ExtendedRequest<T> extendedRequest) {
        return extendedRequest.getCount(databaseClient)
                .map((row, metadata) -> row.get(0, Integer.class))
                .first()
                .defaultIfEmpty(0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Enum<?> parseEnum(Class<?> enumClass, String value) {
        return Enum.valueOf((Class)enumClass, value);
    }

    public Flux<T> findAllExtended(final ExtendedRequest<T> extendedRequest) {
        return extendedRequest.getFindAll(databaseClient)
                .map((row, metadata) -> {
                    // Get a new instance of the Object to return
                    T object = extendedRequest.getModelClassInstance(row, metadata);

                    // Run through all columns
                    for (String columnName : metadata.getColumnNames()) {
                        ColumnMetadata columnMetadata = metadata.getColumnMetadata(columnName);
                        Class<?> c = columnMetadata.getJavaType();
                        // Get the value for the column
                        Object value;

                        if (c == null) {
                            if (DATABASE_INTERVAL_NATIVE_TYPE.equals(columnMetadata.getNativeTypeMetadata())) {
                                // Handle Database Intervals as Java String type
                                c = String.class;
                            } else {
                                throw new DatabaseException("Type for columnName " + columnName + " is null");
                            }
                        }
                        value = row.get(columnName, c);
                        handleColumn(c, object, extendedRequest, columnName, value);
                    }

                    // All columns have been processed - the Object is ready to be returned
                    return object;
                })
                .all();
    }

    private void handleColumn(Class<?> c, T object, ExtendedRequest<T> extendedRequest, String columnName, Object value) {
        try {
            QueryField dbField = extendedRequest.getQueryFieldFromSelectName(columnName);
            Field combinedModelField = dbField.getCombinedModelField();
            Class<?> fieldType;
            if (combinedModelField == null) {
                throw new IllegalStateException("Internal error: Attempting to set a value without a backing field!?");
            }
            fieldType = combinedModelField.getType();
            if (fieldType.isEnum() && value instanceof String) {
                // Convert to an enum
                value = parseEnum(fieldType, (String)value);
                c = fieldType;
            }
            ReflectUtility.setValue(object, combinedModelField.getName(), c, value);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchFieldException exception) {
            if (!extendedRequest.ignoreUnknownProperties()) {
                throw new DatabaseException("Not possible to map value to columnName:" + columnName + " on object " + object.getClass().getSimpleName(), exception);
            }
        }
    }
}
