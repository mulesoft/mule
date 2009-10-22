package org.mule.config.annotations.concept;

import org.mule.config.annotations.routing.Router;
import org.mule.config.annotations.routing.RouterType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Router(type = RouterType.Outbound)
public @interface Splitter
{
    String evaluator();

    String expression();

    boolean deterministic() default true;

    boolean disableRoundRobin() default false;

    boolean failIfNoMatch() default true;

    /**
     * An expression filter used to filter out unwanted messages. Filters can be used for content-based routing.
     * The filter syntax uses familiar Mule expression syntax:
     * <code>
     * filter = "#[wildcard:*.txt]"
     * </code>
     * or
     * <code>
     * filter = "#[xpath:count(Batch/Trade/GUID) > 0]"
     * </code>
     *
     * Filter expressions must result in a boolean or null to mean false
     *
     * @return
     */
    String filter() default "";
}
