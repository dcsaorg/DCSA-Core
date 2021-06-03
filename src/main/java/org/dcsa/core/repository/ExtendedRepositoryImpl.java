package org.dcsa.core.repository;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.query.DBEntityAnalysis;
import org.dcsa.core.query.impl.AbstractQueryFactory;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ExtendedRepositoryImpl<T, I> extends SimpleR2dbcRepository<T, I> implements ExtendedRepository<T, I> {
    private final DatabaseClient databaseClient;
    private final MappingRelationalEntityInformation<T, I> mappingRelationalEntityInformation;
    private final RowMapper rowMapper = new RowMapper();

    public ExtendedRepositoryImpl(MappingRelationalEntityInformation<T, I> mappingRelationalEntityInformation,
                                  R2dbcEntityTemplate r2dbcEntityTemplate,
                                  MappingR2dbcConverter mappingR2dbcConverter) {
        super(mappingRelationalEntityInformation, r2dbcEntityTemplate, mappingR2dbcConverter);
        this.databaseClient = r2dbcEntityTemplate.getDatabaseClient();
        this.mappingRelationalEntityInformation = mappingRelationalEntityInformation;
    }

    /* internal */
    public I getIdOfEntity(T entity) {
        return mappingRelationalEntityInformation.getId(entity);
    }

    public Mono<Integer> countAllExtended(final ExtendedRequest<T> extendedRequest) {
        return databaseClient.sql(extendedRequest.generateCountQuery())
                .map((row, metadata) -> row.get(0, Integer.class))
                .first()
                .defaultIfEmpty(0);
    }

    public Flux<T> findAllExtended(final ExtendedRequest<T> extendedRequest) {
        boolean ignoreUnknownProperties = extendedRequest.ignoreUnknownProperties();
        return executeQuery(extendedRequest, ignoreUnknownProperties).all();
    }

    public Flux<T> findAllQuery(final AbstractQueryFactory<T> queryFactory) {
        return executeQuery(queryFactory, false).all();
    }

    public Mono<T> findQuery(final AbstractQueryFactory<T> queryFactory) {
        return executeQuery(queryFactory, false).one();
    }

    private RowsFetchSpec<T> executeQuery(final AbstractQueryFactory<T> queryFactory, boolean ignoreUnknownProperties) {
        DBEntityAnalysis<T> dbEntityAnalysis = queryFactory.getDbEntityAnalysis();

        return databaseClient.sql(queryFactory.generateSelectQuery())
                .map((row, metadata) ->
                        rowMapper.mapRow(row, metadata, dbEntityAnalysis,
                                dbEntityAnalysis.getEntityType(), ignoreUnknownProperties)
                );
    }
}
