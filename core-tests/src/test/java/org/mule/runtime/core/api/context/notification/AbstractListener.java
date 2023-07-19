/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.NotificationListener;

public abstract class AbstractListener<T extends AbstractServerNotification> implements NotificationListener<T> {

  private T notification = null;

  @Override
  public void onNotification(T notification) {
    this.notification = notification;
  }

  public boolean isNotified() {
    return null != notification;
  }

}
