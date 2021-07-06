/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.profiling.notification;

import org.mule.api.annotation.Experimental;
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

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_SCHEDULING_OPERATION_EXECUTION =
      ProcessingStrategyProfilingEventType.of("PS_SCHEDULING_OPERATION_EXECUTION", CORE_NAMESPACE);

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> STARTING_OPERATION_EXECUTION =
      ProcessingStrategyProfilingEventType.of("STARTING_OPERATION_EXECUTION", CORE_NAMESPACE);

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> OPERATION_EXECUTED =
      ProcessingStrategyProfilingEventType.of("OPERATION_EXECUTED", CORE_NAMESPACE);

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_FLOW_MESSAGE_PASSING =
      ProcessingStrategyProfilingEventType.of("PS_FLOW_MESSAGE_PASSING", CORE_NAMESPACE);

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> PS_SCHEDULING_FLOW_EXECUTION =
      ProcessingStrategyProfilingEventType.of("PS_SCHEDULING_FLOW_EXECUTION", CORE_NAMESPACE);

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> STARTING_FLOW_EXECUTION =
      ProcessingStrategyProfilingEventType.of("STARTING_FLOW_EXECUTION", CORE_NAMESPACE);

  public static final ProfilingEventType<ProcessingStrategyProfilingEventContext> FLOW_EXECUTED =
      ProcessingStrategyProfilingEventType.of("FLOW_EXECUTED", CORE_NAMESPACE);

  private RuntimeProfilingEventType() {}

}
