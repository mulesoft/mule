/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.util.ClassUtils;

public final class LifecycleObject {

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
