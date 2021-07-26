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

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventType.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventType.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventType.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventType.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventType.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventType.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventType.STARTING_OPERATION_EXECUTION;

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

  static {

    registerAction("test", TEST_NOTIFICATION_ID);

    registerAction(
                   STARTING_FLOW_EXECUTION.getProfilingEventTypeNamespace() + "-"
                       + FLOW_EXECUTED.getProfilingEventTypeIdentifier(),
                   STARTING_FLOW_EXECUTION_ID);

    registerAction(PS_SCHEDULING_FLOW_EXECUTION.getProfilingEventTypeNamespace() + "-" + PS_SCHEDULING_FLOW_EXECUTION
        .getProfilingEventTypeIdentifier(),
                   PS_SCHEDULING_FLOW_EXECUTION_ID);

    registerAction(FLOW_EXECUTED.getProfilingEventTypeNamespace() + "-" + FLOW_EXECUTED.getProfilingEventTypeIdentifier(),
                   FLOW_EXECUTED_ID);

    registerAction(FLOW_EXECUTED.getProfilingEventTypeNamespace() + "-" + FLOW_EXECUTED.getProfilingEventTypeIdentifier(),
                   FLOW_EXECUTED_ID);

    registerAction(PS_SCHEDULING_OPERATION_EXECUTION.getProfilingEventTypeNamespace() + "-" + PS_SCHEDULING_OPERATION_EXECUTION
        .getProfilingEventTypeIdentifier(),
                   PS_SCHEDULING_OPERATION_EXECUTION_ID);

    registerAction(PS_SCHEDULING_OPERATION_EXECUTION.getProfilingEventTypeNamespace() + "-" + PS_SCHEDULING_OPERATION_EXECUTION
        .getProfilingEventTypeIdentifier(),
                   PS_SCHEDULING_OPERATION_EXECUTION_ID);

    registerAction(STARTING_OPERATION_EXECUTION.getProfilingEventTypeNamespace() + "-" + STARTING_OPERATION_EXECUTION
        .getProfilingEventTypeIdentifier(),
                   STARTING_OPERATION_EXECUTION_ID);

    registerAction(OPERATION_EXECUTED.getProfilingEventTypeNamespace() + "-" + OPERATION_EXECUTED
        .getProfilingEventTypeIdentifier(),
                   OPERATION_EXECUTED_ID);

    registerAction(PS_FLOW_MESSAGE_PASSING.getProfilingEventTypeNamespace() + "-" + PS_FLOW_MESSAGE_PASSING
        .getProfilingEventTypeIdentifier(),
                   PS_FLOW_MESSAGE_PASSING_ID);
  }

  private ProfilingEventType<?> profilingEventType;

  public <T extends ProfilingEventContext> ProfilingNotification(T profilingEventContext,
                                                                 ProfilingEventType<T> profilingEventType) {
    super(profilingEventContext, getActionId(profilingEventType.getProfilingEventTypeIdentifier()));
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
  public ProfilingEventType<?> getProfilingEventType() {
    return profilingEventType;
  }
}
