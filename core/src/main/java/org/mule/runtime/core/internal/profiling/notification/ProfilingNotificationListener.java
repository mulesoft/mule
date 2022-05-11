/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
