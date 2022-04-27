package org.dcsa.core.service.impl;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.repository.ExtendedRepository;
import org.dcsa.core.service.QueryService;
import reactor.core.publisher.Flux;

public abstract class QueryServiceImpl<R extends ExtendedRepository<T, I>, T, I> extends QueryServiceImplSupport<R, T, I> implements QueryService<T, I> {

  @Override
  public Flux<T> findAllExtended(ExtendedRequest<T> extendedRequest) {
    return getRepository().countAllExtended(extendedRequest)
      .doOnNext(extendedRequest::setQueryCount)
      .thenMany(getRepository().findAllExtended(extendedRequest)
      );
  }
}
