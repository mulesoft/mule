package org.mule.config.annotations.routing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An inbound router that can forward every message to another destination as defined
 * in the "endpoint" property. This can be a logical destination of a URI. <p/> A
 * filter can be applied to this router so that only events matching a criteria will
 * be tapped.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Router(type = RouterType.Inbound)
public @interface WireTap   
{
    /**
     * The name or URI address of the endpoint to use. The URI would be literal, which is not recommended.
     * Instead, you can use either the name of a global endpoint that is already available in the registry,
     * or you can use a property placeholder and the real value will be injected at runtime. For example:
     * <code>@WireTap(endpoint = "${my.endpoint}")</code>
     * The endpoint would then be resolved to a property called 'my.endpoint' that is regeistered with the registry.
     * Note that the endpoint for a wire tap is always asynchronous.
     * @return A string representation of the endpoint URI, name, or property placeholder.
     */
    String endpoint();


    /**
     * The connector name that will be used to receive events. It is important that
     * the endpoint protocol and the connector correlate. For example, if your endpoint is a JMS
     * queue, your connector must be a JMS connector.
     * Many transports such as HTTP do not need a connector to be present, since Mule can create
     * a default one as needed.
     *
     * @return the connector name associated with the endpoint
     */
    String connectorName() default "";

    /**
     * Decides the encoding to be used for events received by this endpoint.
     *
     * @return the encoding set on the endpoint or null if no coding has been
     *         specified
     */
    String encoding() default "";

    /**
     * A comma-separated list of key/value pairs, e.g.,
     * <code>"apple=green, banana=yellow"</code>
     * Property placeholders can be used in these values:
     * <code>"apple=${apple.color}, banana=yellow"</code>
     *
     * @return A comma-separated list of key/value pairs or an empty string if no
     * properties are set.
     */
    String properties() default "";

    /**
     * Transformers are responsible for transforming data when it is sent after the
     * annotated service has finished processing.
     * Transformers should be listed as a comma-separated list of registered transformers.
     * Property placeholders can be used to load transforms based on external values.
     * @return the transformers to use when receiving data
     */
    String transformers() default "";

    /**
     * An expression filter used to filter out unwanted messages. Filters can be used for content-based routing.
     * The filter syntax uses familiar Mule expression
     * syntax:
     * <code>
     * filter = "#[wildcard:*.txt]"
     * </code>
     * or
     * <code>
     * filter = "#[xpath:count(Batch/Trade/GUID) > 0]"
     * </code>
     *
     * Filter expressions must result in a boolean or null to mean false.
     *
     * @return
     */
    String filter() default "";
}
