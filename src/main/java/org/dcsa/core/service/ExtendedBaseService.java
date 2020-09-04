package org.dcsa.core.service;

import org.dcsa.core.util.ExtendedRequest;
import reactor.core.publisher.Flux;

public interface ExtendedBaseService<T, I> extends BaseService<T, I> {
    Class<T> getModelClass();
    Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest);
}
