/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.diagnostics.notification;

import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.core.api.diagnostics.ProfilingDataConsumer;
import org.mule.runtime.core.api.diagnostics.ProfilingEventContext;
import org.mule.runtime.core.api.diagnostics.consumer.context.ProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.api.diagnostics.notification.ProfilerNotificationListener;
import org.mule.runtime.core.api.diagnostics.notification.ProfilingNotification;

/**
 * a {@link NotificationListener} that listens for profiling notifications.
 */
public class DefaultProfilingNotificationListener implements ProfilerNotificationListener<ProfilingNotification> {

  private final ProfilingDataConsumer<ProfilingEventContext> dataConsumer;

  @Override
  public boolean isBlocking() {
    return false;
  }

  public DefaultProfilingNotificationListener(ProfilingDataConsumer<ProfilingEventContext> dataConsumer) {
    this.dataConsumer = dataConsumer;
  }

  @Override
  public void onNotification(ProfilingNotification notification) {
    ProfilingEventContext profilingEventContext = (ProcessingStrategyProfilingEventContext) notification.getSource();
    dataConsumer.onProfilingEvent(notification.getActionName(), profilingEventContext);
  }
}
