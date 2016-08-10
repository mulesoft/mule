/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.core.context.notification.ManagementNotification;


/**
 * <code>ManagementNotificationListener</code> is an observer interface that
 * objects can use to receive notifications about the state of the Mule instance and
 * its resources
 */
public interface ManagementNotificationListener<T extends ManagementNotification> extends ServerNotificationListener<ManagementNotification>
{
    // no methods
}
