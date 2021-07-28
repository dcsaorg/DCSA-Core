package org.dcsa.core.service.impl;

import org.dcsa.core.exception.NotFoundException;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class BaseRepositoryBackedServiceImpl<R extends R2dbcRepository<T, I>, T, I> extends BaseServiceImpl<T, I> {

    public abstract R getRepository();
    public abstract I getIdOfEntity(T entity);

    @Override
    public Flux<T> findAll() {
        return getRepository().findAll();
    }

    @Override
    public Mono<T> findById(final I id) {
        return getRepository().findById(id)
                .switchIfEmpty(Mono.error(new NotFoundException("No " + getType() + " was found with id: " + id)));
    }

    @Transactional
    @Override
    public Mono<T> create(T t) {
        return Mono.just(t)
                .flatMap(this::preCreateHook)
                .flatMap(this::save);
    }

    protected Mono<T> save(T t) {
        return Mono.just(t)
                .flatMap(this::preSaveHook)
                .flatMap(getRepository()::save);
    }

    @Transactional
    @Override
    public Mono<T> update(final T update) {
        return findById(getIdOfEntity(update))
                .flatMap(current -> this.preUpdateHook(current, update))
                .flatMap(this::save);
    }

    @Transactional
    @Override
    public Mono<Void> deleteById(I id) {
        return this.findById(id)
                .flatMap(this::preDeleteHook)
                .flatMap(getRepository()::delete);
    }

    @Transactional
    @Override
    public Mono<Void> delete(T t) {
        return findById(getIdOfEntity(t))
                .flatMap(this::preDeleteHook)
                .flatMap(getRepository()::delete);
    }

    /**
     * A hook for subclasses that need a hook before *any* attempt to save the model instance.
     *
     * @param t The instance about to saved.
     * @return The method must return its argument (possibly modified) as Mono, which will be saved
     *         or an error (e.g. via {@link Mono#error(Throwable)}).
     */
    protected Mono<T> preSaveHook(T t) {
        return Mono.just(t);
    }


    /**
     * A hook for subclasses that need a hook before *any* attempt to save a newly created model instance.
     *
     * This will be run <i>before</i> the {@link #preSaveHook(Object)} and can contain create specific logic
     * (if any).
     *
     * @param t The instance about to saved.
     * @return The method must return its argument (possibly modified) as Mono, which will be saved
     *         or an error (e.g. via {@link Mono#error(Throwable)}).
     */
    protected Mono<T> preCreateHook(T t) {
        return Mono.just(t);
    }

    /**
     * A hook for subclasses that need a hook before *any* attempt to save an updated model instance.
     *
     * This will be run <i>before</i> the {@link #preSaveHook(Object)} and can contain update specific logic
     * (if any).
     *
     * @param current The copy of the instance in the database.
     * @param update The instance provided externally with the changes.
     * @return The method must return exactly one of the arguments (possibly modified) as Mono or an error
     * (e.g. via {@link Mono#error(Throwable)}).  The return value is what will be saved (if it is not an
     * error Mono).
     */
    protected Mono<T> preUpdateHook(T current, T update) {
        return Mono.just(update);
    }

    /**
     * A hook for subclasses that need a hook before *any* attempt to delete a model instance.
     *
     * The hook can prevent the removal by returning an error Mono (e.g. via {@link Mono#error(Throwable)}) or
     * do service specific clean up related to the instance.
     *
     * @param t The instance about to be deleted
     * @return The method must return the argument as a Mono or an error (e.g. via {@link Mono#error(Throwable)}).
     */
    protected Mono<T> preDeleteHook(T t) {
        return Mono.just(t);
    }
}
