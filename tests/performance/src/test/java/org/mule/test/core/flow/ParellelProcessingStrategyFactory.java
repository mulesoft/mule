/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.flow;

import static java.lang.Runtime.getRuntime;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.processor.strategy.WorkQueueProcessingStrategyFactory.TRANSACTIONAL_ERROR_MESSAGE;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * EXPERIMENTAL
 */
public class ParellelProcessingStrategyFactory implements ProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String name) {
    return new ParellelProcessingStrategy();
  }

  static class ParellelProcessingStrategy implements ProcessingStrategy {

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction,
                                                                   MessagingExceptionHandler messagingExceptionHandler) {
      return publisher -> from(publisher)
          .parallel(getRuntime().availableProcessors())
          .doOnNext(assertCanProcess())
          .as(pipelineFunction);
    }

    protected Consumer<Event> assertCanProcess() {
      return event -> {
        if (isTransactionActive()) {
          throw propagate(new DefaultMuleException(createStaticMessage(TRANSACTIONAL_ERROR_MESSAGE)));
        }
      };
    }
  }

}
