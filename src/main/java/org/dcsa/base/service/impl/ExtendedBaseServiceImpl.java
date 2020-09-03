package org.dcsa.base.service.impl;

import org.dcsa.base.model.GetId;
import org.dcsa.base.service.ExtendedBaseService;
import org.dcsa.base.util.ExtendedRequest;
import reactor.core.publisher.Flux;

public abstract class ExtendedBaseServiceImpl<R extends org.dcsa.base.repository.ExtendedRepository<T, I>, T extends GetId<I>, I> extends BaseServiceImpl<R, T, I> implements ExtendedBaseService<T, I> {
    @Override
    public String getType() {
        return getModelClass().getSimpleName();
    }

    public Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest) {
        return getRepository().countAllExtended(extendedRequest).map(
                count -> {
                    extendedRequest.setQueryCount(count.getCount()); return count;
                }).thenMany(
                        getRepository().findAllExtended(extendedRequest)
        );
    }
}
