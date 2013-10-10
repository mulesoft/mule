/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.context.notification;


/**
 * <code>CustomNotificationListener</code> is an observer interface that can be
 * used to listen for Custom notifications using
 * <code>MuleContext.fireCustomEvent(..)</code>. Custom notifications can be used
 * by components and other objects such as routers, transformers, agents, etc to
 * communicate a change of state to each other.
 */
public interface CustomNotificationListener<T extends ServerNotification> extends ServerNotificationListener<ServerNotification>
{
    // no methods
}
