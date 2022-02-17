package org.dcsa.core.service;

/**
 * A service to handle DCSA's query parameters for the GET (all) endpoints.
 *
 * When in doubt, extend {@link QueryService} rather than this service as it is
 * simpler to understand.  Extend this Service when you need a different entity
 * in the database layer than in the controller layer (hench "Asymmetric").
 *
 * Please read the documentation for {@link QueryService} for additional details.
 * The underlying use-case is the same for this class as well - only the asymmetric
 * typing of controller vs. repository layer differs.
 *
 * @param <DM> The data model (from the database layer)
 * @param <TO> The transfer object model (the controller layer)
 * @param <I> The ID type
 */
/* The <I> is strictly speaking unused, but we keep it for consistency (the default implementation
 * requires it anyway for its type constraint)
 */
@SuppressWarnings("unused")
public interface AsymmetricQueryService<DM, TO, I> extends BaseQueryService<DM, TO, I> {
}
