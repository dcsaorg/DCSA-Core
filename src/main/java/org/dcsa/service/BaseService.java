package org.dcsa.service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface BaseService<T, I> {
    Flux<T> findAll();

    Mono<T> findById(I id);
    Mono<T> save(T t);
    Mono<T> update(T t);
    Mono<Void> deleteById(I id);
    Mono<Void> delete(T t);
}
