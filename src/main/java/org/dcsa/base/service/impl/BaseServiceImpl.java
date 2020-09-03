package org.dcsa.base.service.impl;

import org.dcsa.exception.NotFoundException;
import org.dcsa.base.model.GetId;
import org.dcsa.base.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class BaseServiceImpl<R extends R2dbcRepository<T, I>, T extends GetId<I>, I> implements BaseService<T, I> {
    public abstract R getRepository();
    public abstract String getType();

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public Flux<T> findAll() {
        return getRepository().findAll();
    }

    @Override
    public Mono<T> findById(final I id) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("No " + getType() + " was found with id: " + id)));
    }

    @Override
    public Mono<T> save(T t) {
        return getRepository().save(t);
    }

    @Override
    public Mono<T> update(final T t) {
        return findById(t.getId())
                .flatMap(d -> getRepository().save(t));
    }

    @Override
    public Mono<Void> deleteById(I id) {
        return findById(id)
                .flatMap(getRepository()::delete);
    }

    @Override
    public Mono<Void> delete(T t) {
        return findById(t.getId())
                .flatMap(getRepository()::delete);
    }
}
