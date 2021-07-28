package org.dcsa.core.model;

import org.springframework.data.relational.core.sql.Join;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {
    String into() default "";
    String fromFieldName() default "";

    String foreignFieldName();

    String viaJoinAlias() default "";

    Join.JoinType joinType() default Join.JoinType.JOIN;
}
