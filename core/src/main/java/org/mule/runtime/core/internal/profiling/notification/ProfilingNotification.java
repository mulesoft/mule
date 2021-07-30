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

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;

/**
 * A {@link Notification} that produces data for troubleshooting. This is extended for using notifications for producing profiling
 * data by the runtime.
 */
public class ProfilingNotification<T extends ProfilingEventContext> extends AbstractServerNotification {

  private static final int TEST_NOTIFICATION_ID = PROFILING_ACTION_START_RANGE + 1;
  private static final int STARTING_FLOW_EXECUTION_ID = PROFILING_ACTION_START_RANGE + 2;
  private static final int PS_SCHEDULING_FLOW_EXECUTION_ID = PROFILING_ACTION_START_RANGE + 3;
  private static final int FLOW_EXECUTED_ID = PROFILING_ACTION_START_RANGE + 4;
  private static final int PS_SCHEDULING_OPERATION_EXECUTION_ID = PROFILING_ACTION_START_RANGE + 5;
  private static final int STARTING_OPERATION_EXECUTION_ID = PROFILING_ACTION_START_RANGE + 6;
  private static final int OPERATION_EXECUTED_ID = PROFILING_ACTION_START_RANGE + 7;
  private static final int PS_FLOW_MESSAGE_PASSING_ID = PROFILING_ACTION_START_RANGE + 8;

  /**
   * The separator between the profiling identifier and the namespace.
   */
  public static final String PROFILING_NAMESPACE_IDENTIFIER_SEPARATOR = ":";

  static {

    registerAction("test-namespace:test", TEST_NOTIFICATION_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(STARTING_FLOW_EXECUTION), STARTING_FLOW_EXECUTION_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(PS_SCHEDULING_FLOW_EXECUTION),
                   PS_SCHEDULING_FLOW_EXECUTION_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(FLOW_EXECUTED), FLOW_EXECUTED_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(PS_SCHEDULING_OPERATION_EXECUTION),
                   PS_SCHEDULING_OPERATION_EXECUTION_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(STARTING_OPERATION_EXECUTION),
                   STARTING_OPERATION_EXECUTION_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(OPERATION_EXECUTED), OPERATION_EXECUTED_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(PS_FLOW_MESSAGE_PASSING), PS_FLOW_MESSAGE_PASSING_ID);
  }

  private final ProfilingEventType<T> profilingEventType;

  public ProfilingNotification(T profilingEventContext,
                               ProfilingEventType<T> profilingEventType) {
    super(profilingEventContext, getActionId(getFullyQualifiedProfilingNotificationIdentifier(profilingEventType)));
    this.profilingEventType = profilingEventType;
  }

  @Override
  public boolean isSynchronous() {
    return true;
  }

  @Override
  public String getEventName() {
    return "ProfilingServerNotification";
  }

  /**
   * @return the {@link ProfilingEventType} for the notification.
   */
  public ProfilingEventType<T> getProfilingEventType() {
    return profilingEventType;
  }

  /**
   * @return the fully qualified profiling notification identifier considering the namespace.
   */
  public static String getFullyQualifiedProfilingNotificationIdentifier(ProfilingEventType profilingEventType) {
    return profilingEventType.getProfilingEventTypeNamespace() + PROFILING_NAMESPACE_IDENTIFIER_SEPARATOR
        + profilingEventType
            .getProfilingEventTypeIdentifier();
  }
}
