/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.util.ClassUtils;

public class LifecycleObject {

  private Class type;
  private Notification preNotification;
  private Notification postNotification;

  public LifecycleObject(Class type) {
    this.type = type;
  }

  public Notification getPostNotification() {
    return postNotification;
  }

  public void setPostNotification(Notification postNotification) {
    this.postNotification = postNotification;
  }

  public Notification getPreNotification() {
    return preNotification;
  }

  public void setPreNotification(Notification preNotification) {
    this.preNotification = preNotification;
  }

  public Class getType() {
    return type;
  }

  public void setType(Class type) {
    this.type = type;
  }

  public void firePreNotification(NotificationDispatcher notificationFirer) {
    if (preNotification != null) {
      notificationFirer.dispatch(preNotification);
    }
  }

  public void firePostNotification(NotificationDispatcher notificationFirer) {
    if (postNotification != null) {
      notificationFirer.dispatch(postNotification);
    }
  }

  @Override
  public String toString() {
    return super.toString() + " (" + ClassUtils.getSimpleName(type) + ")";
  }

}
