package org.dcsa.core.controller;

import org.dcsa.core.service.QueryService;

/**
 * Provides a controller with a GetMapping (for a list endpoint) that supports DCSA's query parameters/sorting requirements
 *
 * This controller should be used whenever there is a 1:1 between the
 * Transfer model (JSON) and the Data model (SQL) - or when it is close
 * enough and can be expressed by the same model (maybe with a few
 * {@link com.fasterxml.jackson.annotation.JsonIgnore} annotations).
 *
 * If the case requires a distinction between the JSON and the database
 * model, then use the {@link org.dcsa.core.service.AsymmetricQueryService}
 * instead.
 *
 * @param <S> The concrete service that will provide the
 * @param <T> The model class
 * @param <I> The ID type of the model (the type of the field in the model
 *           annotated with {@link org.springframework.data.annotation.Id}).
 */
public abstract class QueryController<S extends QueryService<T, I>, T, I> extends QueryControllerImplSupport<S, T, T, I> {
}
