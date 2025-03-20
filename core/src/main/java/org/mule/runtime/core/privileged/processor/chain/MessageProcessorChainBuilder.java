/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor.chain;


import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.tracer.api.component.ComponentTracer;

/**
 * Builds {@link MessageProcessorChain} instances.
 *
 * @since 3.1
 */
public interface MessageProcessorChainBuilder extends MessageProcessorBuilder {

  /**
   * Chain a {@link Processor} by adding it the the list of processors that the builder implementation will use to construct a
   * {@link MessageProcessorChain}
   *
   * @param processors {@link Processor} instance(s) to be used in the construction of a {@link MessageProcessorChain}
   * @return the current {@link MessageProcessorBuilder} instance.
   */
  MessageProcessorChainBuilder chain(Processor... processors);

  /**
   * Apply a {@link ProcessingStrategy} to the Processors of the target {@link MessageProcessorChain}.
   * 
   * @param processingStrategy the strategy to apply for each {@link Processor} in the target {@link MessageProcessorChain}.
   */
  void setProcessingStrategy(ProcessingStrategy processingStrategy);

  /**
   * Build a new {@link MessageProcessorBuilder}
   *
   * @return a new {@link MessageProcessorBuilder} instance.
   */
  @Override
  MessageProcessorChain build();

  /**
   * @param chainComponentTracer the span customization info for the creation of the
   *                             {@link org.mule.runtime.api.profiling.tracing.Span} associated to the chain.
   * @since 4.5.0
   */
  default void setComponentTracer(ComponentTracer<CoreEvent> chainComponentTracer) {
    // Nothing to do by default.
  }
}
