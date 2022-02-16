package org.dcsa.core.service;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import reactor.core.publisher.Flux;

/* The <I> is strictly speaking unused, but we keep it for consistency (the default implementation
 * requires it anyway
 */
@SuppressWarnings("unused")
public interface ExtendedBaseService<T, I> {
    Class<T> getModelClass();
    Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest);
}
