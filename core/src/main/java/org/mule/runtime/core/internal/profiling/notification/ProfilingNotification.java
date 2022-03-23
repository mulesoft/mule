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
import org.mule.runtime.api.profiling.type.ComponentThreadingProfilingEventType;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentThreadingProfilingEventContext;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.EXTENSION_PROFILING_EVENT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.OPERATION_THREAD_RELEASE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_COMMIT;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_CONTINUE;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_ROLLBACK;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.TX_START;

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
  private static final int COMPONENT_PROFILING_EVENT_ID = PROFILING_ACTION_START_RANGE + 9;
  private static final int STARTING_OPERATION_EXECUTION_EVENT_ID = PROFILING_ACTION_START_RANGE + 10;
  private static final int OPERATION_EXECUTED_EVENT_ID = PROFILING_ACTION_START_RANGE + 11;
  private static final int OPERATION_THREAD_RELEASE_EVENT_ID = PROFILING_ACTION_START_RANGE + 12;
  private static final int TRANSACTION_START_ID = PROFILING_ACTION_START_RANGE + 18;
  private static final int TRANSACTION_CONTINUE_ID = PROFILING_ACTION_START_RANGE + 19;
  private static final int TRANSACTION_COMMIT_ID = PROFILING_ACTION_START_RANGE + 20;
  private static final int TRANSACTION_ROLLBACK_ID = PROFILING_ACTION_START_RANGE + 21;

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

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(PS_STARTING_OPERATION_EXECUTION),
                   STARTING_OPERATION_EXECUTION_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(PS_OPERATION_EXECUTED), OPERATION_EXECUTED_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(PS_FLOW_MESSAGE_PASSING), PS_FLOW_MESSAGE_PASSING_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(EXTENSION_PROFILING_EVENT), COMPONENT_PROFILING_EVENT_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(STARTING_OPERATION_EXECUTION),
                   STARTING_OPERATION_EXECUTION_EVENT_ID);
    registerAction(getFullyQualifiedProfilingNotificationIdentifier(OPERATION_EXECUTED), OPERATION_EXECUTED_EVENT_ID);
    registerAction(getFullyQualifiedProfilingNotificationIdentifier(OPERATION_THREAD_RELEASE), OPERATION_THREAD_RELEASE_EVENT_ID);

    registerAction(getFullyQualifiedProfilingNotificationIdentifier(TX_START), TRANSACTION_START_ID);
    registerAction(getFullyQualifiedProfilingNotificationIdentifier(TX_CONTINUE), TRANSACTION_CONTINUE_ID);
    registerAction(getFullyQualifiedProfilingNotificationIdentifier(TX_COMMIT), TRANSACTION_COMMIT_ID);
    registerAction(getFullyQualifiedProfilingNotificationIdentifier(TX_ROLLBACK), TRANSACTION_ROLLBACK_ID);
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
  public static String getFullyQualifiedProfilingNotificationIdentifier(ProfilingEventType<?> profilingEventType) {
    return profilingEventType.getProfilingEventTypeNamespace() + PROFILING_NAMESPACE_IDENTIFIER_SEPARATOR
        + profilingEventType
            .getProfilingEventTypeIdentifier();
  }
}
