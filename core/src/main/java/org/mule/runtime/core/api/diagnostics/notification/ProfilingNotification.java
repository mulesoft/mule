/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics.notification;

import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.OPERATION_EXECUTED;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_DISPATCH;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_END;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_MESSAGE_PASSING;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.core.api.diagnostics.ProfilingEventContext;
import org.mule.runtime.core.api.diagnostics.ProfilingEventType;

/**
 * A {@link Notification} that produces data for troubleshooting/diagnostics.
 *
 * @since 4.4.0
 */
@Experimental
public class ProfilingNotification extends AbstractServerNotification {

  private static final int DISPATCHING_OPERATION_EXECUTION_ID = PROFILING_ACTION_START_RANGE + 1;
  private static final int OPERATION_EXECUTION_DISPATCHED_ID = PROFILING_ACTION_START_RANGE + 2;
  private static final int DISPATCHING_OPERATION_RESULT_ID = PROFILING_ACTION_START_RANGE + 3;
  private static final int OPERATION_RESULT_DISPATCHED_ID = PROFILING_ACTION_START_RANGE + 4;
  private static final int FLOW_DISPATCH_ID = PROFILING_ACTION_START_RANGE + 5;
  private static final int PS_FLOW_END_ID = PROFILING_ACTION_START_RANGE + 6;

  static {
    registerAction(PS_SCHEDULING_OPERATION_EXECUTION.getProfilingEventName(), DISPATCHING_OPERATION_EXECUTION_ID);
    registerAction(STARTING_OPERATION_EXECUTION.getProfilingEventName(), OPERATION_EXECUTION_DISPATCHED_ID);
    registerAction(OPERATION_EXECUTED.getProfilingEventName(), DISPATCHING_OPERATION_RESULT_ID);
    registerAction(PS_FLOW_MESSAGE_PASSING.getProfilingEventName(), OPERATION_RESULT_DISPATCHED_ID);
    registerAction(PS_FLOW_DISPATCH.getProfilingEventName(), FLOW_DISPATCH_ID);
    registerAction(PS_FLOW_END.getProfilingEventName(), PS_FLOW_END_ID);
  }

  public <T extends ProfilingEventContext> ProfilingNotification(T profilingEventContext,
                                                                 ProfilingEventType<T> profilingEventType) {
    super(profilingEventContext, getActionId(profilingEventType.getProfilingEventName()));
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
