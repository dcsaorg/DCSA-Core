package org.dcsa.core.service;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.query.impl.AbstractQueryFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExtendedBaseService<T, I> extends BaseService<T, I> {
    Class<T> getModelClass();
    Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest);
    Flux<T> findAllQuery(final AbstractQueryFactory<T> queryFactory);
    Mono<T> findQuery(final AbstractQueryFactory<T> queryFactory);
}
