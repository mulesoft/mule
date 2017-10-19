/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static org.mule.runtime.api.notification.EnrichedNotificationInfo.createInfo;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.notification.ConnectorMessageNotification;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.context.notification.OptimisedNotificationHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class to fire notifications of a specified type over a {@link ServerNotificationHandler}.
 */
public class NotificationHelper {

  private static final Logger logger = LoggerFactory.getLogger(NotificationHelper.class);

  private final Class<? extends Notification> notificationClass;
  private final boolean dynamicNotifications;
  private final ServerNotificationHandler defaultNotificationHandler;

  /**
   * Creates a new {@link NotificationHelper} that emits instances of {@code notificationClass} class.
   *
   * @param defaultNotificationHandler The {@link ServerNotificationHandler} to be used on notifications which don't relate to a
   *        {@link CoreEvent}
   * @param notificationClass The {@link Class} of the notifications to be fired by this helper
   * @param dynamicNotifications If {@code true}, notifications will be fired directly to a {@link ServerNotificationHandler}
   *        responsible to decide to emit it or not. If {@code false} the notification will be checked to be enable or not at
   *        creation time
   */
  public NotificationHelper(ServerNotificationHandler defaultNotificationHandler,
                            Class<? extends Notification> notificationClass, boolean dynamicNotifications) {
    this.notificationClass = notificationClass;
    this.dynamicNotifications = dynamicNotifications;
    this.defaultNotificationHandler = adaptNotificationHandler(defaultNotificationHandler);
  }

  /**
   * Checks if the {@link #defaultNotificationHandler} is enabled to fire instances of {@link #notificationClass}.
   *
   * @return {@code true} if {@link #defaultNotificationHandler} is enabled for {@link #notificationClass}
   */
  public boolean isNotificationEnabled() {
    return defaultNotificationHandler.isNotificationEnabled(notificationClass);
  }

  /**
   * Fires a {@link ConnectorMessageNotification} for the given arguments using the {@link ServerNotificationHandler} associated
   * to the given {@code event} and based on a {@link ComponentLocation}.
   *
   * @param source
   * @param event a {@link CoreEvent}
   * @param location the location of the component that generated the notification
   * @param action the action code for the notification
   */
  public void fireNotification(Component source, CoreEvent event, ComponentLocation location, int action) {
    try {
      if (defaultNotificationHandler.isNotificationEnabled(notificationClass)) {
        defaultNotificationHandler
            .fireNotification(new ConnectorMessageNotification(createInfo(event, null, source), location, action));
      }
    } catch (Exception e) {
      logger.warn("Could not fire notification. Action: " + action, e);
    }
  }

  /**
   * Fires the given {@code notification} using the {@link #defaultNotificationHandler}.
   *
   * @param notification a {@link Notification}
   */
  public void fireNotification(Notification notification) {
    defaultNotificationHandler.fireNotification(notification);
  }

  private ServerNotificationHandler adaptNotificationHandler(ServerNotificationHandler serverNotificationHandler) {
    return dynamicNotifications ? serverNotificationHandler
        : new OptimisedNotificationHandler(serverNotificationHandler, notificationClass);
  }

}
