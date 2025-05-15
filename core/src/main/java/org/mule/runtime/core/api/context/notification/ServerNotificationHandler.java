/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;

@NoImplement
public interface ServerNotificationHandler {

  /**
   * Fire the {@link Notification}. Regardless of if a notification is fired synchronously or asynchronously any {@link Throwable}
   * thrown by the {@link NotificationListener} will not be propagated.
   *
   * @param notification the notification to fire.
   */
  void fireNotification(Notification notification);

  boolean isNotificationDynamic();

  /**
   * @since 3.0
   */
  boolean isListenerRegistered(NotificationListener listener);

  /**
   * This returns a very "conservative" value - it is true if the notification or any subclass would be accepted. So if it returns
   * false then you can be sure that there is no need to send the notification. On the other hand, if it returns true there is no
   * guarantee that the notification "really" will be dispatched to any listener.
   *
   * @param notfnClass Either the notification class being generated or some superclass
   * @return false if there is no need to dispatch the notification
   */
  boolean isNotificationEnabled(Class<? extends Notification> notfnClass);

  /**
   * Registers a listener to handle modifications to the server notification configuration.
   * <p>
   * This default implementation does nothing and can be overridden by implementing classes to provide specific functionality.
   * </p>
   *
   * @param serverNotificationConfigurationChangeListener the listener to be registered
   */
  default void registerServerNotificationConfigurationChangeListener(ServerNotificationConfigurationChangeListener serverNotificationConfigurationChangeListener) {
    // Nothing to do.
  }

}
