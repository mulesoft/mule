/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.annotations.endpoints;

import org.mule.config.annotations.routing.Router;
import org.mule.config.annotations.routing.RouterType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that allows developers to configure a reply channel. Mule will wait on the reply channel before
 * returning. Typically, the @Reply annotation is only used when the @Publish annotation is used, since the result of
 * dispatching to a {@link Publish} channel will be returned on the @Reply channel.
 * <p/>
 * The @Reply annotation creates an asynchronous reply channel for a remote service to reply to. In the case of
 * JMS, this would listen on the JMSReplyTo address. In the case of other Mule services, the transport can set up a
 * reply channel.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Channel(identifer = "reply", type = ChannelType.Reply)
@Router(type = RouterType.ReplyTo)
public @interface Reply
{
    /**
     * The name or URI address of the endpoint to use. The URI would be literal, which is not recommended.
     * Instead, you can use either the name of a global endpoint that is already available in the registry,
     * or you can use a property placeholder and the real value will be injected at runtime. For example:
     * <code>@Reply(endpoint = "${my.endpoint}")</code>
     * The endpoint would then be resolved to a property called 'my.endpoint' that is regeistered with the registry.
     * <p/>
     * Expressions can also be used. For example:
     * <code>@Reply(endpoint = "$[mule.message.header(JMSReplyTo)]")</code>
     *
     * @return A string representation of the endpoint URI, name, or property placeholder.
     */
    public String uri();

    /**
     * The connector reference that will be used to create this endpoint. It is important that
     * the endpoint protocol and the connector correlate. For example, if your endpoint is a JMS
     * queue, your connector must be a JMS connector.
     * Many transports such as HTTP do not need a connector to be present, since Mule can create
     * a default one as needed.
     * <p/>
     * The connector reference can be a reference to a connector in the local registry or a reference
     * to an object in Galaxy.
     * <p/>
     * TODO: describe how connectors are created
     *
     * @return the connector name associated with the endpoint
     */
    public String connector() default "";


    /**
     * An expression filter used to filter out unwanted messages. The filter syntax uses familiar Mule expression
     * syntax:
     * <code>
     * filter = "wildcard:*.txt"
     * </code>
     * or
     * <code>
     * filter = "xpath:count(Batch/Trade/GUID) > 0"
     * </code>
     * <p/>
     * Filter expressions must result in a boolean or null to mean false.
     *
     * @return A filter expression string or empty string if not defined.
     */
    public abstract String filter() default "";

    /**
     * The amount of time to wait for a response message in milliseconds.
     *
     * @return the amount of time to wait for a response message in milliseconds
     */
    public int replyTimeout() default 5000;

    /**
     * When false, if no message is received the router will stop waiting without throwing an exception.
     * If a 'callback' method is set, the method will be invoked with a {@link org.mule.transport.NullPayload}.
     * If the expected message is received after the timeout, it will be ignored but Mule will fire a
     * {@link org.mule.context.notification.RoutingNotification.MISSED_ASYNC_REPLY}
     * notification with the received message.
     *
     * @return true if the reply channel will throw an exception if a reply is not received before the timeout elapses.
     */
    public abstract boolean failOnTimeout() default true;

    /**
     * The name of the callback method to call on the annotated class when a reply is received. This callback method can
     * be used to alter any reply messages before they are sent back to the client that initiated the request.
     * This callback method does not have to be defined, and Mule will pass back the reply data to the client.
     *
     * @return The method name to call when a reply is received or empty string if not defined
     */
    public String callback() default "";

    /**
     * An optional identifier for this endpoint. This is only used by Mule to identify the endpoint when logging messages,
     * firing notifications, and for JMX management.
     *
     * @return the name associated with this endpoint
     */
    String id() default "";
}