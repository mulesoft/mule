/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 *  Processing strategy that processes all message processors in the caller thread.
 */
public class CallerThreadProcessingStrategyFactory implements ProcessingStrategyFactory {

  static ProcessingStrategy CALLER_THREAD_PROCESSING_STRATEGY_INSTANCE = new AbstractProcessingStrategy() {

    @Override
    public Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> function) {
      return new Sink() {

        Consumer<Event> onEventConsumer = createOnEventConsumer();

        @Override
        public void accept(Event event) {
          onEventConsumer.accept(event);
          just(event).transform(function).subscribe();
        }

      };
    }

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
    return CALLER_THREAD_PROCESSING_STRATEGY_INSTANCE;
  }

}
