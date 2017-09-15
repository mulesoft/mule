/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.AbstractProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.StreamPerEventSink;

/**
 * Processing strategy that processes the {@link Pipeline} in the caller thread and does not schedule the processing of any
 * {@link Processor} in a different thread pool regardless of their {@link ProcessingType}. While processing of the flow is
 * carried out in the caller thread, when a {@link Processor} implements non-blocking behaviour then processing will continue in a
 * {@link Processor} thread.
 */
public class DirectProcessingStrategyFactory implements ProcessingStrategyFactory {

  public static final ProcessingStrategy DIRECT_PROCESSING_STRATEGY_INSTANCE =
      new AbstractProcessingStrategy() {

        @Override
        public boolean isSynchronous() {
          return true;
        }

        @Override
        public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
          return new StreamPerEventSink(pipeline, event -> {
          });
        }
      };

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return DIRECT_PROCESSING_STRATEGY_INSTANCE;
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return DIRECT_PROCESSING_STRATEGY_INSTANCE.getClass();
  }

}
