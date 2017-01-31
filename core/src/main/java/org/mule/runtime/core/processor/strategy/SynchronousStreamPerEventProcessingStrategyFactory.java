/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.function.Consumer;

/**
 * Processing strategy that processes all message processors in the caller thread.
 */
public class SynchronousStreamPerEventProcessingStrategyFactory implements ProcessingStrategyFactory {

  static ProcessingStrategy SYNCHRONOUS_STREAM_PER_EVENT_PROCESSING_STRATEGY_INSTANCE =
      new AbstractStreamPerEventProcessingStrategyFactory() {

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
    return SYNCHRONOUS_STREAM_PER_EVENT_PROCESSING_STRATEGY_INSTANCE;
  }

}
