/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.function.Consumer;

/**
 * This processing strategy processes all {@link Processor}'s in the caller thread serializing each event using a shared event
 * stream.
 */
public class SynchronousStreamProcessingStrategyFactory implements ProcessingStrategyFactory {

  public static final ProcessingStrategy SYNCHRONOUS_STREAM_PROCESSING_STRATEGY_INSTANCE = new AbstractProcessingStrategy() {

    @Override
    public boolean isSynchronous() {
      return true;
    }

    /*
     * This processing strategy supports transactions so we override default check that fails on transactions.
     */
    @Override
    protected Consumer<Event> createOnEventConsumer() {
      return event -> {
      };
    }
  };

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return SYNCHRONOUS_STREAM_PROCESSING_STRATEGY_INSTANCE;
  }

}
