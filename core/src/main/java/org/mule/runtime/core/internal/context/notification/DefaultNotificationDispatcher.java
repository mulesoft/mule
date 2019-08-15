/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Implementation of {@link NotificationDispatcher} that just forwards events to the {@link MuleContext}.
 * 
 * @since 4.0
 */
public class DefaultNotificationDispatcher implements NotificationDispatcher {

  private static final Logger LOGGER = getLogger(DefaultNotificationDispatcher.class);

  @Inject
  private MuleContext context;

  @Override
  public void dispatch(Notification notification) {
    ServerNotificationManager notificationManager = context.getNotificationManager();
    if (notificationManager != null) {
      notificationManager.fireNotification(notification);
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("MuleEvent Manager is not enabled, ignoring notification: " + notification);
    }
  }

}
