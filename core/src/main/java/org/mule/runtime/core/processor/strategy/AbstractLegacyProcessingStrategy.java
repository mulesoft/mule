/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static reactor.core.Exceptions.propagate;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Interface to be implemented by legacy processing strategy implementations. This interface provides a default implementation of
 * {@link #createSink(FlowConstruct, Function)} that ensures events are processed are not de-multiplexed onto a single
 * {@link org.mule.runtime.core.construct.Flow} stream but are rather executed independenatly stream.
 */
public abstract class AbstractLegacyProcessingStrategy extends AbstractProcessingStrategy {

  @Override
  public Sink createSink(FlowConstruct flowConstruct, Function<Publisher<Event>, Publisher<Event>> function) {
    return new Sink() {

      Consumer<Event> onEventConsumer = createOnEventConsumer();

      @Override
      public void accept(Event event) {
        onEventConsumer.accept(event);
        Mono.just(event).transform(function).subscribe();
      }

      @Override
      public void submit(Event event, Duration duration) {
        onEventConsumer.accept(event);
        accept(event);
      }
    };
  }

}
