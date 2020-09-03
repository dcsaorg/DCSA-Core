package org.dcsa.base.service;

import org.dcsa.base.util.ExtendedRequest;
import reactor.core.publisher.Flux;

public interface ExtendedBaseService<T, I> extends BaseService<T, I> {
    Class<T> getModelClass();
    Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest);
}
