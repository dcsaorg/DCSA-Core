package org.dcsa.core.controller;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.core.service.BaseService;
import org.springframework.http.HttpStatus;
import org.dcsa.core.exception.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@Slf4j
public abstract class BaseController<S extends BaseService<T, I>, T, I> {

    public abstract S getService();
    public abstract String getType();

    @GetMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<T> findById(@PathVariable I id) {
        return getService().findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<T> create(@Valid @RequestBody T t) {
        S s = getService();
        if (s.getIdOfEntity(t) != null) {
            return createMonoError();
        }
        return s.create(t);
    }

    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<T> update(@PathVariable I id, @Valid @RequestBody T t) {
        S s = getService();
        if (!id.equals(s.getIdOfEntity(t))) {
            return updateMonoError();
        }
        return s.update(t);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@RequestBody T t) {
        S s = getService();
        if (s.getIdOfEntity(t) == null) {
            return deleteMonoError();
        }
        return s.delete(t);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteById(@PathVariable I id) {
        return getService().deleteById(id);
    }


    // Error handling

    public Mono<Void> deleteMonoError() {
        return Mono.error(ConcreteRequestErrorMessageException.invalidParameter(getType(), null, "No Id provided in " + getType() + " object"));
    }

    public Mono<T> updateMonoError() {
        return Mono.error(ConcreteRequestErrorMessageException.invalidParameter("Id in url does not match id in body"));
    }

    public Mono<T> createMonoError() {
        return Mono.error(ConcreteRequestErrorMessageException.invalidParameter(getType(), null, "Id not allowed when creating a new " + getType()));
    }
}
