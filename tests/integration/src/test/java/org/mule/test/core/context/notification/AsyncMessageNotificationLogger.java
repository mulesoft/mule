/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.context.notification;

import org.mule.runtime.core.api.context.notification.AsyncMessageNotificationListener;
import org.mule.runtime.core.context.notification.AsyncMessageNotification;

import java.util.List;

public class AsyncMessageNotificationLogger extends PipelineAndAsyncMessageNotificationLogger
    implements AsyncMessageNotificationListener<AsyncMessageNotification>, NotificationLogger {

  @Override
  public boolean isBlocking() {
    return false;
  }

  @Override
  public synchronized void onNotification(AsyncMessageNotification notification) {
    notifications.addLast(notification);
  }

  @Override
  public List getNotifications() {
    return notifications;
  }
}
