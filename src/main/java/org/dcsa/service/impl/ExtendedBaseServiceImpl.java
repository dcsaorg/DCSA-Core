package org.dcsa.service.impl;

import org.dcsa.model.GetId;
import org.dcsa.repository.ExtendedRepository;
import org.dcsa.service.ExtendedBaseService;
import org.dcsa.util.ExtendedRequest;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public abstract class ExtendedBaseServiceImpl<S extends ExtendedRepository<T>, R extends R2dbcRepository<T, I>, T extends GetId<I>, I> extends BaseServiceImpl<R, T, I> implements ExtendedBaseService<T, I> {
    @Override
    public String getType() {
        return getModelClass().getSimpleName();
    }

    public Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest) {
        return ((S) getRepository()).findAllExtended(extendedRequest);
    }
}
