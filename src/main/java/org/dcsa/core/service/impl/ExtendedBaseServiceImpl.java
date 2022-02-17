package org.dcsa.core.service.impl;

import org.dcsa.core.repository.ExtendedRepository;
import org.dcsa.core.service.ExtendedBaseService;

/**
 * Extend {@link QueryServiceImpl} instead.
 */
@Deprecated
public abstract class ExtendedBaseServiceImpl<R extends ExtendedRepository<T, I>, T, I> extends QueryServiceImpl<R, T, I> implements ExtendedBaseService<T, I> {


}
