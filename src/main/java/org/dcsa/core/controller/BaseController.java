package org.dcsa.core.controller;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.core.exception.*;
import org.dcsa.core.model.transferobjects.ConcreteRequestErrorMessageTO;
import org.dcsa.core.model.transferobjects.RequestFailureTO;
import org.dcsa.core.service.BaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.BadSqlGrammarException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

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
        return Mono.error(new DeleteException("No Id provided in " + getType() + " object"));
    }

    public Mono<T> updateMonoError() {
        return Mono.error(new UpdateException("Id in url does not match id in body"));
    }

    public Mono<T> createMonoError() {
        return Mono.error(ConcreteRequestErrorMessageException.invalidParameter(getType(), null, "Id not allowed when creating a new " + getType()));
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
            // - we "should" have handled that by @Size attributes, so this is mostly here as a fallback
            throw ConcreteRequestErrorMessageException.invalidParameter("Trying to update a string value that is too long");
        } else if ("42804".equals(ex.getR2dbcException().getSqlState())) {
            throw ConcreteRequestErrorMessageException.internalServerError("Internal mismatch between backEnd and database.", ex);
        } else {
            throw ConcreteRequestErrorMessageException.internalServerError("Internal error with database operation.", ex);
        }
    }

    @ExceptionHandler(ServerWebInputException.class)
    public void handle(ServerWebInputException ex) {
        throw ConcreteRequestErrorMessageException.invalidParameter(ex.getMessage(), ex);
    }

    @ExceptionHandler(ConcreteRequestErrorMessageException.class)
    public ResponseEntity<RequestFailureTO> handle(HttpServletRequest httpServletRequest, ConcreteRequestErrorMessageException ex) {
        ResponseStatus responseStatusAnnotation = ex.getClass().getAnnotation(ResponseStatus.class);
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        ConcreteRequestErrorMessageTO errorEntity = ex.asConcreteRequestMessage();
        if (responseStatusAnnotation != null) {
            httpStatus = responseStatusAnnotation.code();
        }
        StringBuffer requestUri = httpServletRequest.getRequestURL();
        String query = httpServletRequest.getQueryString();
        if (query != null) {
            requestUri.append('?').append(httpServletRequest.getQueryString());
        }
        RequestFailureTO failureTO = new RequestFailureTO(
                httpServletRequest.getMethod(),
                requestUri.toString(),
                List.of(errorEntity),
                httpStatus
        );
        return new ResponseEntity<>(failureTO, httpStatus);
    }
}
