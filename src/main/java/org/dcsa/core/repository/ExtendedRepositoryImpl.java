package org.dcsa.core.repository;

import io.r2dbc.spi.ColumnMetadata;
import org.dcsa.core.exception.DatabaseException;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.model.Count;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;

public class ExtendedRepositoryImpl<T, I> extends SimpleR2dbcRepository<T, I> implements ExtendedRepository<T, I> {

    private final DatabaseClient databaseClient;

    public ExtendedRepositoryImpl(MappingRelationalEntityInformation<T, I> mappingRelationalEntityInformation,
                                  R2dbcEntityTemplate r2dbcEntityTemplate,
                                  MappingR2dbcConverter mappingR2dbcConverter) {
        super(mappingRelationalEntityInformation, r2dbcEntityTemplate, mappingR2dbcConverter);
        databaseClient = r2dbcEntityTemplate.getDatabaseClient();
    }

    public Mono<Count> countAllExtended(final ExtendedRequest<T> extendedRequest) {
        return databaseClient
                .execute(extendedRequest.getCountQuery()).as(Count.class).fetch().first();
    }

    public Flux<T> findAllExtended(final ExtendedRequest<T> extendedRequest) {
        return databaseClient
                .execute(extendedRequest.getQuery())
                .map((row, meta) -> {
                    // Get a new instance of the Object to return
                    T object = extendedRequest.getModelClassInstance(row, meta);

                    // Run through all columns
                    for (String columnName : meta.getColumnNames()) {
                        ColumnMetadata columnMetadata = meta.getColumnMetadata(columnName);
                        Class<?> c = columnMetadata.getJavaType();
                        if (c != null) {
                            // Get the value for the column
                            Object value = row.get(columnName, c);
                            // Set the value on the Object to return
                            try {
                                ReflectUtility.setValue(object, columnName, c, value);
                            } catch (IllegalAccessException | InvocationTargetException exception) {
                                if (!extendedRequest.ignoreUnknownProperties()) {
                                    throw new DatabaseException("Not possible to map value to columnName:" + columnName + " on object " + object.getClass().getSimpleName(), exception);
                                }
                            }
                        } else {
                            throw new DatabaseException("Type for columnName: " + columnName + " is null");
                        }
                    }

                    // All columns have been processed - the Object is ready to be returned
                    return object;
                })
                .all();
    }
}
