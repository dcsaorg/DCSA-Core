package org.dcsa.core.service.impl;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.repository.ExtendedRepository;
import org.dcsa.core.service.ExtendedBaseService;
import reactor.core.publisher.Flux;

public abstract class ExtendedBaseServiceImpl<R extends ExtendedRepository<T, I>, T, I> extends BaseRepositoryBackedServiceImpl<R, T, I> implements ExtendedBaseService<T, I> {

    public Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest) {
        return getRepository().countAllExtended(extendedRequest)
                .doOnNext(extendedRequest::setQueryCount)
                .thenMany(getRepository().findAllExtended(extendedRequest)
        );
    }
}
