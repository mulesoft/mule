/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.NotificationListener;

/**
 * <code>MuleContextNotificationListener</code> is an observer interface that objects can implement and then register themselves
 * with the Mule manager to be notified when a Manager event occurs.
 */
public interface MuleContextNotificationListener<T extends MuleContextNotification>
    extends NotificationListener<MuleContextNotification> {
  // no methods
}
