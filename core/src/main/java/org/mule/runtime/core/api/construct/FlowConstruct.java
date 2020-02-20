/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.construct;

import static org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory.DIRECT_PROCESSING_STRATEGY_INSTANCE;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.lifecycle.LifecycleStateEnabled;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategySupplier;
import org.mule.runtime.core.api.source.MessageSource;

/**
 * A uniquely identified {@link FlowConstruct} that once implemented and configured defines a construct through which messages are
 * processed using {@link MessageSource} and {@link Processor} building blocks.
 */
public interface FlowConstruct extends NamedObject, LifecycleStateEnabled, ProcessingStrategySupplier, Component {

  /**
   * @return The exception listener that will be used to handle exceptions that may be thrown at different points during the
   *         message flow defined by this construct.
   *
   * @deprecated since 4.3.0. Error handling in the flow is encapsulated.
   */
  @Deprecated
  FlowExceptionHandler getExceptionListener();

  /**
   * @return The statistics holder used by this flow construct to keep track of its activity.
   */
  FlowConstructStatistics getStatistics();

  /**
   * @return This muleContext that this flow construct belongs to and runs in the context of.
   */
  MuleContext getMuleContext();

  /**
   * Generate a unique ID string
   */
  String getUniqueIdString();

  /**
   * @return the id of the running mule server
   */
  String getServerId();

  /**
   * @return the {@link ProcessingStrategy} used.
   */
  @Override
  default ProcessingStrategy getProcessingStrategy() {
    return DIRECT_PROCESSING_STRATEGY_INSTANCE;
  }

  /**
   * Check if backpressure will be fired in the {@link ProcessingStrategy} upon emitting an event. First, the backpressure
   * handling strategy is decided based on some condition (the current implementation takes into account just the
   * {@link MessageSource}, but since this remains decoupled, it's easily extensible). Then, an attempt is made to inject the
   * {@link CoreEvent} into the {@link ProcessingStrategy}, handling the failure with the selected strategy.
   *
   * @param event the event about to be processed
   * @throws RuntimeException
   */
  default void checkBackpressure(CoreEvent event) throws RuntimeException {}

}
