package org.dcsa.core.controller;

import org.dcsa.core.service.QueryService;

/**
 * Extend {@link org.dcsa.core.service.QueryService} (or {@link AsymmetricQueryController}) instead.
 */
@Deprecated
public abstract class ExtendedBaseController<S extends QueryService<T, I>, T, I> extends QueryController<S, T, I> {
}
