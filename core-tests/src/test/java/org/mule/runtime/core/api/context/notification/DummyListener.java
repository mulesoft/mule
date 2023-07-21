/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;

public class DummyListener implements NotificationListener {

  @Override
  public void onNotification(Notification notification) {
    // empty
  }

}
