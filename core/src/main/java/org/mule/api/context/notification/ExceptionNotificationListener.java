/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

import org.mule.context.notification.ExceptionNotification;


/**
 * <code>ExceptionNotificationListener</code> is an observer interface that
 * objects can implement and then register themselves with the Mule manager to be
 * notified when a Exception event occurs.
 */
public interface ExceptionNotificationListener<T extends ExceptionNotification> extends ServerNotificationListener<ExceptionNotification>
{
    // no methods
}
