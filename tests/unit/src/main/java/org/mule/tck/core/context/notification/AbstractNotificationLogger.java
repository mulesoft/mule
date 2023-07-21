/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
