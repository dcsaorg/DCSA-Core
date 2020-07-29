package org.dcsa.controller;

import org.dcsa.exception.*;
import org.dcsa.model.GetId;
import org.dcsa.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.r2dbc.BadSqlGrammarException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public abstract class BaseController<S extends BaseService<T, I>, T extends GetId, I> {

    abstract S getService();
    abstract String getType();

//    @GetMapping()
//    public Flux<T> findAll() {
//        return getService().findAll();
//    }

    @GetMapping("{id}")
    public Mono<T> findById(@PathVariable I id) {
        return getService().findById(id);
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<T> save(@RequestBody T t) {
        if (t.getId() != null) {
            return createMonoError();
        }
        return getService().save(t);
    }

    @PutMapping("{id}")
    public Mono<T> update(@PathVariable I id, @RequestBody T t) {
        if (!id.equals(t.getId())) {
            return updateMonoError();
        }
        return getService().update(t);
    }

    @DeleteMapping()
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(@RequestBody T t) {
        if (t.getId() == null) {
            return deleteMonoError();
        }
        return getService().delete(t);
    }

    @DeleteMapping("{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteById(@PathVariable I id) {
        return getService().deleteById(id);
    }

    // Error handling

    Mono<Void> deleteMonoError() {
        return Mono.error(new DeleteException("No Id provided in " + getType() + " object"));
    }
    Mono<T> updateMonoError() {
        return Mono.error(new UpdateException("Id in url does not match id in body"));
    }
    Mono<T> createMonoError() {
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
        if (ex.getMessage().contains("Invalid UUID string:")) {
                log.warn("Invalid UUID string");
                throw new InvalidParameterException("Input was not a valid UUID format");
        } else {
            log.error(this.getClass().getSimpleName());
            throw ex;
        }
    }
}
