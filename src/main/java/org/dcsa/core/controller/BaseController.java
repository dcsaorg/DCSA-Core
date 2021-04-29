package org.dcsa.core.controller;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.core.exception.*;
import org.dcsa.core.service.BaseService;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebInputException;
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

    @Transactional
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<T> create(@Valid @RequestBody T t) {
        S s = getService();
        if (s.getIdOfEntity(t) != null) {
            return createMonoError();
        }
        return s.create(t);
    }

    @Transactional
    @PutMapping("{id}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<T> update(@PathVariable I id, @Valid @RequestBody T t) {
        S s = getService();
        if (!id.equals(s.getIdOfEntity(t))) {
            return updateMonoError();
        }
        return s.update(t);
    }

    @Transactional
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@RequestBody T t) {
        S s = getService();
        if (s.getIdOfEntity(t) == null) {
            return deleteMonoError();
        }
        return s.delete(t);
    }

    @Transactional
    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteById(@PathVariable I id) {
        return getService().deleteById(id);
    }


    // Error handling

    public Mono<Void> deleteMonoError() {
        return Mono.error(new DeleteException("No Id provided in " + getType() + " object"));
    }

    public Mono<T> updateMonoError() {
        return Mono.error(new UpdateException("Id in url does not match id in body"));
    }

    public Mono<T> createMonoError() {
        return Mono.error(new CreateException("Id not allowed when creating a new " + getType()));
    }

    @ExceptionHandler
    public void handle(Exception ex) throws Exception {
        // log exception
        if (ex instanceof DeleteException || ex instanceof UpdateException || ex instanceof CreateException || ex instanceof NotFoundException) {
            log.debug(this.getClass().getSimpleName() + " (" + ex.getClass().getSimpleName() + ") - " + ex.getMessage());
        } else {
            log.error(this.getClass().getSimpleName() + " error!", ex);
        }
        throw ex;
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public void handle(BadSqlGrammarException ex) {
        if ("22001".equals(ex.getR2dbcException().getSqlState())) {
            // The error with code 22001 is thrown when trying to insert a value that is too long for the column
            if (ex.getSql().startsWith("INSERT INTO")) {
                log.warn(this.getClass().getSimpleName() + " insert into error! - " + ex.getR2dbcException().getMessage());
                throw new CreateException("Trying to insert a string value that is too long");
            } else {
                log.warn(this.getClass().getSimpleName() + " update error! - " + ex.getR2dbcException().getMessage());
                throw new UpdateException("Trying to update a string value that is too long");
            }
        } else if ("42804".equals(ex.getR2dbcException().getSqlState())) {
            log.error(this.getClass().getSimpleName() + " database error! - " + ex.getR2dbcException().getMessage());
            throw new DatabaseException("Internal mismatch between backEnd and database - please see log");
        } else {
            log.error(this.getClass().getSimpleName() + " R2dbcException!", ex);
            throw new DatabaseException("Internal error with database operation - please see log");
        }
    }
    @ExceptionHandler(ServerWebInputException.class)
    public void handle(ServerWebInputException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("Invalid UUID string:")) {
            log.warn("Invalid UUID string");
            throw new InvalidParameterException("Input was not a valid UUID format");
        } else {
            log.error(this.getClass().getSimpleName());
            throw ex;
        }
    }
}
