/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.consumer;

import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_OPERATION_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_STARTING_OPERATION_EXECUTION;

import static com.google.common.collect.ImmutableSet.of;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.api.profiling.type.context.ComponentProcessingStrategyProfilingEventContext;
import org.mule.runtime.core.internal.profiling.consumer.annotations.RuntimeInternalProfilingDataConsumer;
import org.mule.runtime.core.internal.profiling.consumer.operations.EndSpanProfilingExecutionOpertion;
import org.mule.runtime.core.internal.profiling.consumer.operations.LoggerProfilingEventOperation;
import org.mule.runtime.core.internal.profiling.consumer.operations.ProfilingExecutionOperation;
import org.mule.runtime.core.internal.profiling.consumer.operations.StartSpanProfilingExecutionOperation;

import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableMultimap;
import org.slf4j.Logger;

/**
 * A {@link ProfilingDataConsumer} that performs operations related to the component processing strategy.
 */
@RuntimeInternalProfilingDataConsumer
public class ComponentProcessingStrategyDataConsumer
    implements ProfilingDataConsumer<ComponentProcessingStrategyProfilingEventContext> {

  private static final Logger LOGGER = getLogger(ComponentProcessingStrategyDataConsumer.class);
  private final ImmutableMultimap<ProfilingEventType<ComponentProcessingStrategyProfilingEventContext>, ProfilingExecutionOperation<ComponentProcessingStrategyProfilingEventContext>> operations;
  private final ProfilingService profilingService;
  private final Logger logger;

  public ComponentProcessingStrategyDataConsumer(ProfilingService profilingService) {
    this(profilingService, LOGGER);
  }

  public ComponentProcessingStrategyDataConsumer(ProfilingService profilingService, Logger logger) {
    this.profilingService = profilingService;
    this.logger = logger;
    this.operations =
        ImmutableMultimap
            .<ProfilingEventType<ComponentProcessingStrategyProfilingEventContext>, ProfilingExecutionOperation<ComponentProcessingStrategyProfilingEventContext>>builder()
            .put(PS_SCHEDULING_OPERATION_EXECUTION, new LoggerProfilingEventOperation(logger, PS_SCHEDULING_OPERATION_EXECUTION))
            .put(PS_STARTING_OPERATION_EXECUTION, new LoggerProfilingEventOperation(logger, PS_STARTING_OPERATION_EXECUTION))
            .put(PS_OPERATION_EXECUTED, new LoggerProfilingEventOperation(logger, PS_OPERATION_EXECUTED))
            .put(PS_FLOW_MESSAGE_PASSING, new LoggerProfilingEventOperation(logger, PS_FLOW_MESSAGE_PASSING))
            .put(PS_SCHEDULING_FLOW_EXECUTION, new LoggerProfilingEventOperation(logger, PS_SCHEDULING_FLOW_EXECUTION))
            .put(STARTING_FLOW_EXECUTION, new LoggerProfilingEventOperation(logger, STARTING_FLOW_EXECUTION))
            .put(FLOW_EXECUTED, new LoggerProfilingEventOperation(logger, FLOW_EXECUTED))
            .put(STARTING_FLOW_EXECUTION, new StartSpanProfilingExecutionOperation(profilingService))
            .put(FLOW_EXECUTED, new EndSpanProfilingExecutionOpertion(profilingService))
            .put(PS_SCHEDULING_OPERATION_EXECUTION, new StartSpanProfilingExecutionOperation(profilingService))
            .put(PS_FLOW_MESSAGE_PASSING, new EndSpanProfilingExecutionOpertion(profilingService))
            .build();
  }

  @Override
  public void onProfilingEvent(ProfilingEventType<ComponentProcessingStrategyProfilingEventContext> profilingEventType,
                               ComponentProcessingStrategyProfilingEventContext profilingEventContext) {
    operations.get(profilingEventType).forEach(operation -> operation.execute(profilingEventContext));
  }

  @Override
  public Set<ProfilingEventType<ComponentProcessingStrategyProfilingEventContext>> getProfilingEventTypes() {
    return of(PS_SCHEDULING_OPERATION_EXECUTION, PS_STARTING_OPERATION_EXECUTION, PS_OPERATION_EXECUTED,
              PS_FLOW_MESSAGE_PASSING, PS_SCHEDULING_FLOW_EXECUTION, STARTING_FLOW_EXECUTION,
              FLOW_EXECUTED);
  }

  @Override
  public Predicate<ComponentProcessingStrategyProfilingEventContext> getEventContextFilter() {
    return processingStrategyProfilingEventContext -> true;
  }

  /**
   * @return the logger used for consuming the profiling data.
   */
  protected Logger getDataConsumerLogger() {
    return logger;
  }
}
