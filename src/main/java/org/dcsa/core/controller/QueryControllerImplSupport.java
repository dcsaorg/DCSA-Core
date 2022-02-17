package org.dcsa.core.controller;

import org.dcsa.core.exception.GetException;
import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.service.BaseQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

/*
 * Implementation detail of the (Asymmetric)QueryController
 */
abstract class QueryControllerImplSupport<S extends BaseQueryService<DM, TO, I>, DM, TO, I> {

    @Autowired
    private ExtendedParameters extendedParameters;

    @Autowired
    protected R2dbcDialect r2dbcDialect;

    /**
     * @return The concrete service instance. Usually this is a
     * trivial getter for the service field.
     */
    protected abstract S getService();

    /**
     * Provides the {@link ExtendedRequest} object used for the filter/sorting support
     *
     * This method can be overridden to provide a custom {@link ExtendedRequest}
     * implementation if the default does not work.
     *
     * @return The ExtendedRequest object which will be used for the querying.
     */
    protected ExtendedRequest<DM> newExtendedRequest() {
        return new ExtendedRequest<>(extendedParameters, r2dbcDialect, getService().getModelClass());
    }

    /**
     * Provides the "GET (all)" endpoint with DCSA filter, sorting and cursor support.
     *
     * Usually, you do <b>not</b> need to override this method.
     *
     * @param response The server http response object (injected by Spring)
     * @param request The server http request object (injected by Spring)
     * @return A flux of the result.
     */
    @GetMapping
    public Flux<TO> findAll(ServerHttpResponse response, ServerHttpRequest request) {
        ExtendedRequest<DM> extendedRequest = newExtendedRequest();
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
