package org.dcsa.core.controller;

import org.dcsa.core.model.GetId;
import org.dcsa.core.service.ExtendedBaseService;
import org.dcsa.core.extendedrequest.ExtendedParameters;
import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.exception.GetException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

import java.util.Map;

public abstract class ExtendedBaseController<S extends ExtendedBaseService<T, I>, T extends GetId<I>, I> extends BaseController<S, T, I> {

    @Autowired
    private ExtendedParameters extendedParameters;

    @Override
    public String getType() {
        return getService().getModelClass().getSimpleName();
    }

    @GetMapping()
    public Flux<T> findAll(ServerHttpResponse response, ServerHttpRequest request) {
        ExtendedRequest<T> extendedRequest = new ExtendedRequest<>(extendedParameters, getService().getModelClass());
        try {
            Map<String,String> params = request.getQueryParams().toSingleValueMap();
            extendedRequest.parseParameter(params);
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
