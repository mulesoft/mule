/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.profiling.notification;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.api.profiling.consumer.context.ProcessingStrategyProfilingEventContext;

/**
 * {@link ProfilingEventType}'s associated to the Runtime {@link ProfilingEventType}'s.
 *
 * @since 4.4.0
 */
@Experimental
public class RuntimeProfilingEventType {

  private static final String CORE_NAMESPACE = "CORE";

  /**
   * A {@link ProfilingEventType} that indicates that a {@link CoreEvent} has reached the processing strategy that orchestrates an
   * operation's execution. The processing strategy is scheduling the execution of the operation, which may involve a thread
   * switch.
   *
   * @since 4.4
   */
  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_SCHEDULING_OPERATION_EXECUTION =
      ProcessingStrategyProfilingEventType.of("PS_SCHEDULING_OPERATION_EXECUTION", CORE_NAMESPACE);

  /**
   * A {@link ProfilingEventType} that indicates that an operation is about to begin its execution. This boundary is expressed as
   * broadly as possible, and will include, for instance, the execution of the interceptors defined for the operation.
   *
   * @since 4.4
   */
  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> STARTING_OPERATION_EXECUTION =
      ProcessingStrategyProfilingEventType.of("STARTING_OPERATION_EXECUTION", CORE_NAMESPACE);

  /**
   * A {@link ProfilingEventType} that indicates that an operation has finished its execution, and the processing strategy must
   * resolve the message passing of the resultant {@link CoreEvent} to the flow which may involve a thread switch.
   *
   * @since 4.4
   */
  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> OPERATION_EXECUTED =
      ProcessingStrategyProfilingEventType.of("OPERATION_EXECUTED", CORE_NAMESPACE);

  /**
   * A {@link ProfilingEventType} that indicates that the processing strategy has executed the message passing and is handling the
   * control back to the flow.
   *
   * @since 4.4
   */
  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_FLOW_MESSAGE_PASSING =
      ProcessingStrategyProfilingEventType.of("PS_FLOW_MESSAGE_PASSING", CORE_NAMESPACE);

  /**
   * A {@link ProfilingEventType} that indicates that a {@link CoreEvent} reached the processing strategy that orchestrates a
   * flow's execution. The processing strategy is scheduling the execution of the flow, which may involve a thread switch.
   *
   * @since 4.4
   */
  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_SCHEDULING_FLOW_EXECUTION =
      ProcessingStrategyProfilingEventType.of("PS_SCHEDULING_FLOW_EXECUTION", CORE_NAMESPACE);

  /**
   * A {@link ProfilingEventType} that indicates that the flow is about to begin its execution.
   *
   * @since 4.4
   */
  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> STARTING_FLOW_EXECUTION =
      ProcessingStrategyProfilingEventType.of("STARTING_FLOW_EXECUTION", CORE_NAMESPACE);

  /**
   * A {@link ProfilingEventType} that indicates that the flow has finished its execution.
   *
   * @since 4.4
   */
  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> FLOW_EXECUTED =
      ProcessingStrategyProfilingEventType.of("FLOW_EXECUTED", CORE_NAMESPACE);

  private RuntimeProfilingEventType() {}

}
