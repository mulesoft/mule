/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.remoting.notification;

import org.mule.api.context.notification.ServerNotificationListener;

/**
 * <code>ManagementNotificationListener</code> is an observer interface that
 * objects can use to receive notifications about the state of the Mule instance and
 * its resources
 */
@Deprecated
public interface RemoteDispatcherNotificationListener<T extends RemoteDispatcherNotification> extends ServerNotificationListener<RemoteDispatcherNotification>
{
    // no methods
}
