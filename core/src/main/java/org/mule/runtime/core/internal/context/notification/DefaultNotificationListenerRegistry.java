/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.serverNotificationManagerNotEnabled;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;

import java.util.function.Predicate;

import javax.inject.Inject;

/**
 * Implementation of {@link NotificationListenerRegistry} registers listeners using a ServerNotificationHandler implementation.
 * 
 * @since 4.0
 */
public class DefaultNotificationListenerRegistry implements NotificationListenerRegistry {

  @Inject
  private MuleContext context;

  @Override
  public <N extends Notification> void registerListener(NotificationListener<N> listener) {
    requireNonNull(context.getNotificationManager(), serverNotificationManagerNotEnabled().getMessage());
    context.getNotificationManager().addListener(listener);
  }

  @Override
  public <N extends Notification> void registerListener(NotificationListener<N> listener, Predicate<N> selector) {
    requireNonNull(context.getNotificationManager(), serverNotificationManagerNotEnabled().getMessage());
    requireNonNull(selector);
    context.getNotificationManager().addListenerSubscription(listener, selector);
  }

  @Override
  public <N extends Notification> void unregisterListener(NotificationListener<N> listener) {
    if (context.getNotificationManager() != null) {
      context.getNotificationManager().removeListener(listener);
    }
  }

}
