package org.dcsa.base.repository;

import org.dcsa.base.model.Count;
import org.dcsa.base.util.ExtendedRequest;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface ExtendedRepository<T, I> extends R2dbcRepository<T, I> {
    Mono<Count> countAllExtended(final ExtendedRequest<T> extendedRequest);
    Flux<T> findAllExtended(final ExtendedRequest<T> extendedRequest);
}
