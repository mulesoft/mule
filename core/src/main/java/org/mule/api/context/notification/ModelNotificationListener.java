/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

import org.mule.context.notification.ModelNotification;


/**
 * <code>ModelNotificationListener</code> is an observer interface that objects can
 * implement and then register themselves with the Mule manager to be notified when a
 * Model event occurs.
 */
@Deprecated
public interface ModelNotificationListener<T extends ModelNotification> extends ServerNotificationListener<ModelNotification>
{
    // no methods
}
