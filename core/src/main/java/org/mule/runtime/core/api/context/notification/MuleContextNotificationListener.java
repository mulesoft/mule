/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
