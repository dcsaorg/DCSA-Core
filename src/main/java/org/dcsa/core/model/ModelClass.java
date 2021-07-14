package org.dcsa.core.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Deprecated
/**
 * @deprecated Not supported any more. Might be fixed in the future.
 */
public @interface ModelClass {
    Class<?> value() default Object.class;
    String fieldName() default "";
    String viaJoinAlias() default "";
}
