package org.dcsa.repository;

import org.dcsa.util.ExtendedRequest;
import reactor.core.publisher.Flux;

public interface ExtendedRepository<T> {
    Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest);
}
