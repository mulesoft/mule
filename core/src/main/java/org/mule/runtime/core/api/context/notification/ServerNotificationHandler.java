/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;

public interface ServerNotificationHandler {

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

}
