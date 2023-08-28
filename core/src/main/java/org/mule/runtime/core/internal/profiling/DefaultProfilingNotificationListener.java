/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling;

import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.core.internal.profiling.notification.ProfilingNotificationListener;
import org.mule.runtime.core.internal.profiling.notification.ProfilingNotification;

/**
 * A {@link NotificationListener} that listens for profiling notifications.
 *
 * @since 4.4
 */
public class DefaultProfilingNotificationListener<T extends ProfilingEventContext> implements ProfilingNotificationListener<T> {

  private final ProfilingDataConsumer<T> dataConsumer;

  @Override
  public boolean isBlocking() {
    return false;
  }

  @Override
  public void onNotification(ProfilingNotification<T> notification) {
    dataConsumer.onProfilingEvent(notification.getProfilingEventType(), (T) notification.getSource());
  }

  public DefaultProfilingNotificationListener(ProfilingDataConsumer<T> dataConsumer) {
    this.dataConsumer = dataConsumer;
  }
}
