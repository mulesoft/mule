/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling.notification;

import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.profiling.ProfilingEventContext;

/**
 * An interface for {@link ProfilingNotification} listeners. This is implemented by the runtime to use notifications for producing
 * profiling data.
 *
 * @param <T> extension of {@link ProfilingNotification}
 */
public interface ProfilingNotificationListener<T extends ProfilingEventContext>
    extends NotificationListener<ProfilingNotification<T>> {

}
