/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.util.Pair;

import java.util.Map;

/**
 * Plugins may implement this interface and register that implementation in the bootstrap to declare the notification types it
 * supports.
 *
 * @since 4.0
 */
@NoImplement
public interface NotificationsProvider {

  /**
   * The key of the returned map is the string representation of the notification. It must be in the format
   * {@code [artifactid]:[NOTIFICATION-ID]}
   * 
   * @return the mapping of the notification ID to the concrete types of {@link Notification} and {@link NotificationListener} it
   *         represents.
   */
  Map<String, Pair<Class<? extends Notification>, Class<? extends NotificationListener>>> getEventListenerMapping();
}
