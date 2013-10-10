/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.client.remoting.notification;

import org.mule.api.context.notification.ServerNotificationListener;

/**
 * <code>ManagementNotificationListener</code> is an observer interface that
 * objects can use to receive notifications about the state of the Mule instance and
 * its resources
 */
public interface RemoteDispatcherNotificationListener<T extends RemoteDispatcherNotification> extends ServerNotificationListener<RemoteDispatcherNotification>
{
    // no methods
}
