/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

import org.mule.context.notification.ConnectionNotification;


/**
 * <code>ConnectionNotificationListener</code> is an observer interface that
 * objects can implement and then register themselves with the Mule manager to be
 * notified when a Connection event occurs.
 */
public interface ConnectionNotificationListener<T extends ConnectionNotification> extends ServerNotificationListener<ConnectionNotification>
{
    // no methods
}
