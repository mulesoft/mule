/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.context.notification;

import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationHandler;

/**
 * Optimized to make a quick decision on a particular class of messages.
 */
public class OptimisedNotificationHandler implements ServerNotificationHandler {

  private ServerNotificationHandler delegate;
  private Class<? extends Notification> type;
  private boolean dynamic = false;
  private volatile Boolean enabled = null;

  public OptimisedNotificationHandler(ServerNotificationHandler delegate, Class<? extends Notification> type) {
    this.delegate = delegate;
    this.type = type;
    dynamic = delegate.isNotificationDynamic();
  }

  @Override
  public boolean isNotificationDynamic() {
    return dynamic;
  }

  @Override
  public boolean isListenerRegistered(NotificationListener listener) {
    return delegate.isListenerRegistered(listener);
  }

  /**
   * This returns a very "conservative" value - it is true if the notification or any subclass would be accepted. So if it returns
   * false then you can be sure that there is no need to send the notification. On the other hand, if it returns true there is no
   * guarantee that the notification "really" will be dispatched to any listener.
   *
   * @param notfnClass Either the notification class being generated or some superclass
   * @return false if there is no need to dispatch the notification
   */
  @Override
  public boolean isNotificationEnabled(Class<? extends Notification> notfnClass) {
    if ((!dynamic) && type.isAssignableFrom(notfnClass)) {
      if (enabled == null) {
        enabled = delegate.isNotificationEnabled(notfnClass);
      }

      return enabled;
    } else {
      return delegate.isNotificationEnabled(notfnClass);
    }
  }

  @Override
  public void fireNotification(Notification notification) {
    if (isNotificationEnabled(notification.getClass())) {
      delegate.fireNotification(notification);
    }
  }
}
