package org.dcsa.core.service.impl;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.model.GetId;
import org.dcsa.core.repository.ExtendedRepository;
import org.dcsa.core.service.ExtendedBaseService;
import reactor.core.publisher.Flux;

public abstract class ExtendedBaseServiceImpl<R extends ExtendedRepository<T, I>, T extends GetId<I>, I> extends BaseServiceImpl<R, T, I> implements ExtendedBaseService<T, I> {
    @Override
    public String getType() {
        return getModelClass().getSimpleName();
    }

    public Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest) {
        return getRepository().countAllExtended(extendedRequest).map(
                count -> {
                    extendedRequest.setQueryCount(count); return count;
                }).thenMany(
                        getRepository().findAllExtended(extendedRequest)
        );
    }
}
