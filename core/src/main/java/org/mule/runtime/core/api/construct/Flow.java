/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.construct;

import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
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
public interface Flow extends AnnotatedObject, Lifecycle, Pipeline, Processor {

  String INITIAL_STATE_STOPPED = "stopped";
  String INITIAL_STATE_STARTED = "started";

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
    Builder messagingExceptionHandler(MessagingExceptionHandler exceptionListener);

    /**
     * Configures the factory used to create processing strategies on the created flow.
     *
     * @param processingStrategyFactory factory to create processing strategies. Non null.
     * @return same builder instance.
     */
    Builder processingStrategyFactory(ProcessingStrategyFactory processingStrategyFactory);

    /**
     * Usually a flow is started automatically ("started"), but this attribute can be used to disable initial startup ("stopped").
     * 
     * @param initialState The initial state of the flow. Non null.
     * @return same builder instance.
     */
    Builder initialState(String initialState);

    /**
     * Builds a flow with the provided configuration.
     *
     * @return a new flow instance.
     */
    Flow build();

  }
}
