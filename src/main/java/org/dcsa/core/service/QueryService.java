package org.dcsa.core.service;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.dcsa.core.service.impl.QueryServiceImpl;

/**
 * A service to handle DCSA's query parameters for the GET (all) endpoints.
 *
 * The {@link QueryService} (and its base implementation {@link QueryServiceImpl})
 * provides a way to handle the query parameters for a GET (all) endpoint via the
 * {@link #findAllExtended(ExtendedRequest)} method.
 *
 * The {@link ExtendedRequest} (or a subclass thereof) provides additional context
 * to the database extraction (along with some request state).
 *
 * The service should be combined with a QueryController, which provides the
 * GET endpoint and leverages this service (and its default implementation)
 * to provide the result.
 *
 * @param <T> The transfer object model (the controller layer)
 * @param <I> The ID type
 */
/* The <I> is strictly speaking unused, but we keep it for consistency (the default implementation
 * requires it anyway for its type constraint)
 */
@SuppressWarnings("unused")
public interface QueryService<T, I> extends BaseQueryService<T, T, I> {
}
