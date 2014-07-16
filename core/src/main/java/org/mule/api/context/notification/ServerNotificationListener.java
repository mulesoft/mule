/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

/**
 * <code>ServerNotificationListener</code> is an observer interface that ojects
 * can implement and register themselves with the Mule Server to receive
 * notifications when the server, model and components stop, start, initialise, etc.
 */
public interface ServerNotificationListener<T extends ServerNotification>
{
    void onNotification(T notification);
}
