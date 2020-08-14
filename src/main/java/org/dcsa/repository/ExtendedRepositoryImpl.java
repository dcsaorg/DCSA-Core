package org.dcsa.repository;

import org.dcsa.exception.DatabaseException;
import org.dcsa.util.ExtendedRequest;
import org.dcsa.util.ReflectUtility;
import io.r2dbc.spi.ColumnMetadata;
import io.r2dbc.spi.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Flux;

import java.lang.reflect.InvocationTargetException;

@RequiredArgsConstructor
public class ExtendedRepositoryImpl<T> implements ExtendedRepository<T> {

    private final ConnectionFactory connectionFactory;

    public Flux<T> findAllExtended(final ExtendedRequest<T> extendedRequest) {
        return DatabaseClient.create(connectionFactory)
                .execute(extendedRequest.getQuery())
                .map((row, meta) -> {
                    // Get a new instance of the Object to return
                    T object = extendedRequest.getModelClassInstance();

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
                                throw new DatabaseException("Not possible to map value to columnName:" + columnName + " on object " + object.getClass().getSimpleName(), exception);
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
