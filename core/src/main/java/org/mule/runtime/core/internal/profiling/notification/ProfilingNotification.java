/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.notification;

import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;

/**
 * A {@link Notification} that produces data for troubleshooting. This is extended for using notifications for producing profiling
 * data by the runtime.
 */
public class ProfilingNotification extends AbstractServerNotification {

  private static final int TEST_NOTIFICATION_ID = PROFILING_ACTION_START_RANGE + 1;

  static {
    registerAction("test", TEST_NOTIFICATION_ID);
  }

  public <T extends ProfilingEventContext> ProfilingNotification(T profilingEventContext,
                                                                 ProfilingEventType<T> profilingEventType) {
    super(profilingEventContext, getActionId(profilingEventType.getProfilingEventTypeIdentifier()));
  }

  @Override
  public boolean isSynchronous() {
    return true;
  }

  @Override
  public String getEventName() {
    return "ProfilingServerNotification";
  }

}
