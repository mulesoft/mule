/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.lifecycle;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.notification.ClusterNodeNotification;
import org.mule.runtime.api.notification.ClusterNodeNotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * This class will start an Startable mule object that must only be started in the primary node.
 *
 */
public class PrimaryNodeLifecycleNotificationListener implements ClusterNodeNotificationListener {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());
  private Startable startMeOnPrimaryNodeNotification;
  private NotificationListenerRegistry notificationListenerRegistry;

  public PrimaryNodeLifecycleNotificationListener(Startable startMeOnPrimaryNodeNotification,
                                                  NotificationListenerRegistry notificationListenerRegistry) {
    this.startMeOnPrimaryNodeNotification = startMeOnPrimaryNodeNotification;
    this.notificationListenerRegistry = notificationListenerRegistry;
  }

  public void register() {
    if (notificationListenerRegistry != null) {
      notificationListenerRegistry.registerListener(this);
    }
  }

  @Override
  public void onNotification(ClusterNodeNotification notification) {
    try {
      if (startMeOnPrimaryNodeNotification instanceof LifecycleState) {
        if (((LifecycleState) startMeOnPrimaryNodeNotification).isStarted()) {
          startMeOnPrimaryNodeNotification.start();
        } else {
          logStartableNotStartedMessage();
        }
      } else if (startMeOnPrimaryNodeNotification instanceof LifecycleStateEnabled) {
        if (((LifecycleStateEnabled) startMeOnPrimaryNodeNotification).getLifecycleState().isStarted()) {
          startMeOnPrimaryNodeNotification.start();
        } else {
          logStartableNotStartedMessage();
        }
      } else {
        startMeOnPrimaryNodeNotification.start();
      }
    } catch (MuleException e) {
      throw new RuntimeException("Error starting wrapped message source", e);
    }
  }

  private void logStartableNotStartedMessage() {
    if (logger.isDebugEnabled()) {
      logger.debug("Not starting Startable since it's not in started state");
    }
  }

  public void unregister() {
    notificationListenerRegistry.unregisterListener(this);
  }
}
