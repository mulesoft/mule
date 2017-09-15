/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static reactor.core.publisher.BlockingSink.Emission.BACKPRESSURED;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.event.BaseEventContext;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import reactor.core.publisher.BlockingSink;
import reactor.core.publisher.FluxSink;

/**
 * Abstract base {@link ProcessingStrategy} that creates a basic {@link Sink} that serializes events.
 */
public abstract class AbstractProcessingStrategy implements ProcessingStrategy {

  public static final String TRANSACTIONAL_ERROR_MESSAGE = "Unable to process a transactional flow asynchronously";

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    return new DirectSink(pipeline, createOnEventConsumer());
  }

  protected Consumer<BaseEvent> createOnEventConsumer() {
    return event -> {
      if (isTransactionActive()) {
        ((BaseEventContext) event.getContext()).error(new MessagingException(event,
                                                                             new DefaultMuleException(createStaticMessage(TRANSACTIONAL_ERROR_MESSAGE))));
      }
    };
  }

  protected ExecutorService decorateScheduler(Scheduler scheduler) {
    return scheduler;
  }

  /**
   * Implementation of {@link Sink} using Reactor's {@link FluxSink} to accept events.
   */
  static final class ReactorSink implements Sink, Disposable {

    private final BlockingSink<BaseEvent> blockingSink;
    private final reactor.core.Disposable disposable;
    private final Consumer onEventConsumer;

    ReactorSink(BlockingSink<BaseEvent> blockingSink, reactor.core.Disposable disposable,
                Consumer<BaseEvent> onEventConsumer) {
      this.blockingSink = blockingSink;
      this.disposable = disposable;
      this.onEventConsumer = onEventConsumer;
    }

    @Override
    public void accept(BaseEvent event) {
      onEventConsumer.accept(event);
      blockingSink.accept(event);
    }

    @Override
    public boolean emit(BaseEvent event) {
      onEventConsumer.accept(event);
      return blockingSink.emit(event) != BACKPRESSURED;
    }

    @Override
    public void dispose() {
      blockingSink.complete();
      disposable.dispose();
    }

  }
}
