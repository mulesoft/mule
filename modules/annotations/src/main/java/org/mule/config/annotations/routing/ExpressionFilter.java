package org.mule.config.annotations.routing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Router(type = RouterType.Inbound)
public @interface ExpressionFilter
{
    /**
     * The Mule expression to filter on
     * @return
     */
    public String value();
}
