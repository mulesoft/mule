/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static org.mule.runtime.core.api.config.i18n.CoreMessages.serverNotificationManagerNotEnabled;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;

import java.util.function.Predicate;

import jakarta.inject.Inject;

/**
 * Implementation of {@link NotificationListenerRegistry} registers listeners using a ServerNotificationHandler implementation.
 *
 * @since 4.0
 */
public class DefaultNotificationListenerRegistry implements NotificationListenerRegistry {

  private ServerNotificationManager notificationManager;

  @Override
  public <N extends Notification> void registerListener(NotificationListener<N> listener) {
    requireNonNull(notificationManager, serverNotificationManagerNotEnabled().getMessage());
    notificationManager.addListener(listener);
  }

  @Override
  public <N extends Notification> void registerListener(NotificationListener<N> listener, Predicate<N> selector) {
    requireNonNull(notificationManager, serverNotificationManagerNotEnabled().getMessage());
    requireNonNull(selector);
    notificationManager.addListenerSubscription(listener, selector);
  }

  @Override
  public <N extends Notification> void unregisterListener(NotificationListener<N> listener) {
    if (notificationManager != null) {
      notificationManager.removeListener(listener);
    }
  }

  @Inject
  public void setNotificationManager(ServerNotificationManager notificationManager) {
    this.notificationManager = notificationManager;
  }
}
