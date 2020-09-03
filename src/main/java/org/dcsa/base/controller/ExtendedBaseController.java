package org.dcsa.base.controller;

import lombok.extern.slf4j.Slf4j;
import org.dcsa.base.model.GetId;
import org.dcsa.base.service.ExtendedBaseService;
import org.dcsa.base.util.ExtendedParameters;
import org.dcsa.base.util.ExtendedRequest;
import org.dcsa.exception.GetException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
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
            Map<String,String> params = request.getQueryParams().toSingleValueMap();;
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
