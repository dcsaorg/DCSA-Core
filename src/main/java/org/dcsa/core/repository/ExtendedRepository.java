package org.dcsa.core.repository;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.query.impl.AbstractQueryFactory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.NoRepositoryBean;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface ExtendedRepository<T, I> extends R2dbcRepository<T, I> {
    Mono<Integer> countAllExtended(final ExtendedRequest<T> extendedRequest);
    Flux<T> findAllExtended(final ExtendedRequest<T> extendedRequest);
    /* internal */ I getIdOfEntity(T entity);

    Flux<T> findAllQuery(final AbstractQueryFactory<T> queryFactory);
    Mono<T> findQuery(final AbstractQueryFactory<T> queryFactory);

}
