/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
