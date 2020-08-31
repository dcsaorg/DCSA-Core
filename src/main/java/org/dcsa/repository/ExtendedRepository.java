package org.dcsa.repository;

import org.dcsa.model.Count;
import org.dcsa.util.ExtendedRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ExtendedRepository<T> {
    Mono<Count> countAllExtended(final ExtendedRequest<T> extendedRequest);
    Flux<T> findAllExtended(final ExtendedRequest<T> extendedRequest);
}
