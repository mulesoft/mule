/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics.notification;

import org.mule.runtime.core.api.diagnostics.ProfilingEventType;
import org.mule.runtime.core.api.diagnostics.consumer.context.ProcessingStrategyProfilingEventContext;

/**
 * Profiling event types for the runtime
 *
 * @since 4.4.0
 */
public class RuntimeProfilingEventType {

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_SCHEDULING_OPERATION_EXECUTION =
      DefaultProfilingEventType.of("ps_scheduling_operation_execution");

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> STARTING_OPERATION_EXECUTION =
      DefaultProfilingEventType.of("starting_operation_execution");

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> OPERATION_EXECUTED =
      DefaultProfilingEventType.of("operation_executed");

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_FLOW_MESSAGE_PASSING =
      DefaultProfilingEventType.of("ps_flow_message_passing");

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_FLOW_DISPATCH =
      DefaultProfilingEventType.of("ps_flow_dispatch");

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_FLOW_END =
      DefaultProfilingEventType.of("ps_flow_end");

  private RuntimeProfilingEventType() {}

}
