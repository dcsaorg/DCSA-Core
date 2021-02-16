package org.dcsa.core.model;

import org.springframework.data.relational.core.sql.Join;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ModelJoins.class)
public @interface JoinedWithModel {
    /* Model to join with (right hand side) */
    Class<?> rhsModel();

    /* Model to join from (left hand side).
     * Either this or lhsJoinAlias must be given.
     *
     * The use of Object.class is a sentinel to enable it to be omitted by default.
     */
    Class<?> lhsModel() default Object.class;

    /* If either side is joined more than once, these fields can be used to
     * avoid name clashes.
     */
    String rhsJoinAlias() default "";
    String lhsJoinAlias() default "";

    String rhsFieldName();
    String lhsFieldName();

    Join.JoinType joinType() default Join.JoinType.JOIN;

    String[] filterFields() default {};
}
