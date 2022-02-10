package org.dcsa.core.model;

import org.dcsa.core.extendedrequest.ExtendedRequest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Make {@link ExtendedRequest} populate this entity
 *
 * <b>Note</b>: In most cases, you want {@link ForeignKey} instead of this
 * annotation.
 *
 * The entity must be provided in the underlying SQL query
 * (usually via @{@link JoinedWithModel}).  Note that in many
 * cases, this annotation + {@link JoinedWithModel} can often
 * be replaced by {@link ForeignKey}.  This annotation should
 * only be used if you need something complex that
 * {@link ForeignKey} cannot support (like pulling an entity
 * in over an intermediate link without mapping out the
 * intermediate entity).
 *
 * This annotation only works with 1:1 (JOIN) and 1:0 (LEFT JOIN)
 * like relations.
 *
 * Please see {@link JoinedWithModel} for an example
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MapEntity {
    String joinAlias() default "";
}
