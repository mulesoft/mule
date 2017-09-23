/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.core.context.notification;

import org.mule.runtime.api.notification.Notification;

import java.util.LinkedList;
import java.util.List;

/**
 * Generic support for notification listeners that store the received notifications to run assertions on them.
 * 
 * @param <T> the type of notifications to store.
 * @since 4.0
 */
public abstract class AbstractNotificationLogger<T extends Notification> implements NotificationLogger<T> {

  private LinkedList<T> notifications = new LinkedList<>();

  public synchronized void onNotification(T notification) {
    notifications.addLast(notification);
  }

  @Override
  public List<T> getNotifications() {
    return notifications;
  }

}
