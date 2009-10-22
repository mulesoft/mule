package org.mule.config.annotations.endpoints;

import org.mule.impl.endpoint.MEP;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An EndpointBinding annotation allows for endpoints to be bound to an interface method, so that when the method
 * on the interface is called, the endpoint will be invoked. Annotating a class field with this means that a proxy object
 * will be injected as the field member. The field cannot be final but can be private. There is no need to have bean getter
 * and setter methods for this field.
 * <p>If this annotatated field enforces Java language access control, and the underlying field is inaccessible, the method throws an
 * <code>IllegalAccessException</code>.
 *
 * @deprecated use an ibean instead. Keeping this around in case there are some valid reasons to use it we have not thought of yet.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Channel(identifer = "binding", type = ChannelType.Binding)
@SupportedMEPs({MEP.OutOnly, MEP.OutIn})
public @interface Bind
{
    /**
     * The name or URI address of the endpoint to use. The URI would be literal, which is not recommended.
     * Instead, you can use either the name of a global endpoint that is already available in the registry,
     * or you can use a property placeholder and the real value will be injected at runtime. For example:
     * <code>@EndpointBinding(endpoint = "${my.endpoint}")</code>
     * The endpoint would then be resolved to a property called 'my.endpoint' that is registered with the registry.
     *
     * @return A string representation of the endpoint URI, name, or property placeholder.
     */
    public abstract String uri();

    /**
     * The connector reference that will be used to create this endpoint. It is important that
     * the endpoint protocol and the connector correlate. For example, if your endpoint is a JMS
     * queue, your connector must be a JMS connector.
     * Many transports such as HTTP do not need a connector to be present, since Mule can create
     * a default one as needed.
     * <p/>
     * The connector reference can be a reference to a connector in the local registry or a reference
     * to an object in galaxy.
     * <p/>
     * TODO: describe how connectors are created
     *
     * @return the connector name associated with the endpoint
     */
    public abstract String connector() default "";

    /**
     * The method to called on the bound interface. This argument can be omitted if there is
     * only one method on the bound interface.
     * <p/>
     * TODO not usre this is needed
     *
     * @return the method name to call or an empty string if it has not been set
     */
    public abstract String method() default "";

    /**
     * An optional identifier for this endpoint. This is only used by Mule to identify the endpoint when logging messages,
     * firing notifications, and for JMX management.
     *
     * @return the name associated with this endpoint
     */
    public abstract String id() default "";
}