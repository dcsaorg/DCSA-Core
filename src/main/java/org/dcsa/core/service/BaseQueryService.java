package org.dcsa.core.service;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import reactor.core.publisher.Flux;


/**
 * Implementation-detail of (Asymmetric)QueryService to facilitate
 * the controller handling.
 *
 * Use {@link AsymmetricQueryService} / {@link QueryService} instead
 * of this.
 */
@SuppressWarnings("unused")
public interface BaseQueryService<DM, TO, I> {
    /**
     * Public as an implementation detail.  Do not use this method.
     */
    Class<DM> getModelClass();
    Flux<TO> findAllExtended(ExtendedRequest<DM> extendedRequest);
}
