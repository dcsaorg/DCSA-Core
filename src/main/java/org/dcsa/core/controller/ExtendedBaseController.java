package org.dcsa.core.controller;

import org.dcsa.core.exception.GetException;
import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.service.ExtendedBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

public abstract class ExtendedBaseController<S extends ExtendedBaseService<T, I>, T, I> {

    @Autowired
    private ExtendedParameters extendedParameters;

    @Autowired
    protected R2dbcDialect r2dbcDialect;

    protected abstract S getService();

    protected ExtendedRequest<T> newExtendedRequest() {
        return new ExtendedRequest<>(extendedParameters, r2dbcDialect, getService().getModelClass());
    }

    @GetMapping
    public Flux<T> findAll(ServerHttpResponse response, ServerHttpRequest request) {
        ExtendedRequest<T> extendedRequest = newExtendedRequest();
        try {
            extendedRequest.parseParameter(request.getQueryParams());
        } catch (GetException getException) {
            return Flux.error(getException);
        }

        return getService().findAllExtended(extendedRequest).doOnComplete(
                () -> {
                    // Add Link headers to the response
                    extendedRequest.insertHeaders(response, request);
                }
        );
    }
}
