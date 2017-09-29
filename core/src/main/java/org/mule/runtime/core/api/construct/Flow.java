/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.construct;

import org.mule.runtime.api.component.execution.ExecutableComponent;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.DefaultFlowBuilder;

import java.util.List;

/**
 * Defines a {@link Pipeline} that represents a Mule flow.
 * <p/>
 * A flow adds the following behaviour to a {@link Pipeline}
 * <ul>
 * <li>Rejects inbound events when Flow is not started</li>
 * <li>Gathers statistics and processing time data</li>
 * <li>Implements MessageProcessor allowing direct invocation of the pipeline</li>
 * <li>Supports the optional configuration of a {@link ProcessingStrategy} that determines how message processors are processed.
 * </ul>
 */
public interface Flow extends ExecutableComponent, Lifecycle, Pipeline, Processor {

  String INITIAL_STATE_STOPPED = "stopped";
  String INITIAL_STATE_STARTED = "started";

  /**
   * @return initial state of the flow, which can be {@value INITIAL_STATE_STARTED} or {@value INITIAL_STATE_STOPPED}
   */
  String getInitialState();

  /**
   * Creates a new flow builder
   *
   * @param name name of the flow to be created. Non empty.
   * @param muleContext context where the flow will be registered. Non null.
   */
  static Builder builder(String name, MuleContext muleContext) {
    return new DefaultFlowBuilder(name, muleContext);
  }

  interface Builder {

    /**
     * Configures the message source for the flow.
     *
     * @param messageSource message source to use. Non null.
     * @return same builder instance.
     */
    Builder source(MessageSource messageSource);

    /**
     * Configures the message processors to execute as part of flow.
     *
     * @param processors message processors to execute. Non null.
     * @return same builder instance.
     */
    Builder processors(List<Processor> processors);

    /**
     * Configures the message processors to execute as part of flow.
     *
     * @param processors message processors to execute.
     * @return same builder instance.
     */
    Builder processors(Processor... processors);

    /**
     * Configures the exception listener to manage exceptions thrown on the flow execution.
     *
     * @param exceptionListener exception listener to use on the flow. Non null.
     * @return same builder instance
     */
    Builder messagingExceptionHandler(FlowExceptionHandler exceptionListener);

    /**
     * Configures the factory used to create processing strategies on the created flow.
     *
     * @param processingStrategyFactory factory to create processing strategies. Non null.
     * @return same builder instance.
     */
    Builder processingStrategyFactory(ProcessingStrategyFactory processingStrategyFactory);

    /**
     * Configures a {@link org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory} as the processing
     * strategy on the flow.
     *
     * @return same builder instance.
     */
    // TODO(pablo.kraan): MULE-13545 - added as a workaround to avoid exposing DirectProcessingStrategyFactory on the API
    Builder withDirectProcessingStrategyFactory();

    /**
     * Usually a flow is started automatically ("started"), but this attribute can be used to disable initial startup ("stopped").
     * 
     * @param initialState The initial state of the flow. Non null.
     * @return same builder instance.
     */
    Builder initialState(String initialState);

    /**
     * Configures the maximum permitted concurrency of the {@link Flow}. This value determines the maximum level of
     * parallelism that the Flow can use to optimize for performance when processing messages. Note that this does not impact in
     * any way the number of threads that a {@link MessageSource} may use to invoke a {@link Flow} and so if a direct or blocking
     * {@link ProcessingStrategy} is used where processing occurs in source threads it is actually the source that defines maximum
     * concurrency.
     * 
     * @param maxConcurrency
     * @return
     */
    Builder maxConcurrency(int maxConcurrency);

    /**
     * Builds a flow with the provided configuration.
     *
     * @return a new flow instance.
     */
    Flow build();

  }
}
