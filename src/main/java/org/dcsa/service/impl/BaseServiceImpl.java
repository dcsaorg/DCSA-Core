package org.dcsa.service.impl;

import org.dcsa.exception.NotFoundException;
import org.dcsa.model.GetId;
import org.dcsa.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class BaseServiceImpl<R extends ReactiveCrudRepository<T, I>, T extends GetId<I>, I> implements BaseService<T, I> {
    abstract R getRepository();
    abstract String getType();

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
