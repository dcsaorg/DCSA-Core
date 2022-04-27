package org.dcsa.core.service.impl;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.repository.ExtendedRepository;
import org.dcsa.core.service.AsymmetricQueryService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public abstract class AsymmetricQueryServiceImpl<R extends ExtendedRepository<DM, I>, DM, TO, I> extends QueryServiceImplSupport<R, DM, I> implements AsymmetricQueryService<DM, TO, I> {

  public Flux<TO> findAllExtended(ExtendedRequest<DM> extendedRequest) {
    return getRepository().countAllExtended(extendedRequest)
      .doOnNext(extendedRequest::setQueryCount)
      .thenMany(bulkMapDM2TO(getRepository().findAllExtended(extendedRequest))
      );
  }

  /**
   * Method to bulk map DM instances into TO instances
   *
   * By default, this delegates all logic to {@link #mapDM2TO(Object)}.
   *
   * You can provide a custom implementation if it is faster than mapping the
   * objects one at the time (e.g., by bulking calls to the database).
   */
  protected Flux<TO> bulkMapDM2TO(Flux<DM> dmFlux) {
    return dmFlux.concatMap(this::mapDM2TO);
  }

  /**
   * Method to map a single DM instance into a TO instance
   */
  protected abstract Mono<TO> mapDM2TO(DM dm);


}
