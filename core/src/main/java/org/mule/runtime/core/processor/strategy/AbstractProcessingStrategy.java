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

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.exception.MessagingException;

import java.util.function.Consumer;

import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.FluxProcessor;
import reactor.core.publisher.FluxSink;

/**
 * Abstract base {@link ProcessingStrategy} that creates a basic {@link Sink} that serializes events.
 */
public abstract class AbstractProcessingStrategy implements ProcessingStrategy {

  public static final String TRANSACTIONAL_ERROR_MESSAGE = "Unable to process a transactional flow asynchronously";

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    FluxProcessor<Event, Event> processor = EmitterProcessor.<Event>create(false).serialize();
    return new ReactorSink(processor.sink(), processor.transform(pipeline).retry().subscribe(),
                           createOnEventConsumer());
  }

  protected Consumer<Event> createOnEventConsumer() {
    return event -> {
      if (isTransactionActive()) {
        event.getContext().error(new MessagingException(event,
                                                        new DefaultMuleException(createStaticMessage(TRANSACTIONAL_ERROR_MESSAGE))));
      }
    };
  }

  /**
   * Implementation of {@link Sink} using Reactor's {@link FluxSink} to accept events.
   */
  static final class ReactorSink implements Sink, Disposable {

    private final FluxSink<Event> fluxSink;
    private final reactor.core.Disposable disposable;
    private final Consumer onEventConsumer;

    ReactorSink(FluxSink<Event> fluxSink, reactor.core.Disposable disposable,
                Consumer<Event> onEventConsumer) {
      this.fluxSink = fluxSink;
      this.disposable = disposable;
      this.onEventConsumer = onEventConsumer;
    }

    @Override
    public void accept(Event event) {
      onEventConsumer.accept(event);
      // TODO MULE-11449 Implement handling of back-pressure via OVERLOAD exception type.
      fluxSink.next(event);
    }

    @Override
    public void dispose() {
      fluxSink.complete();
      disposable.dispose();
    }

  }
}
