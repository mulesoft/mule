/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.CORES;
import static reactor.util.concurrent.Queues.SMALL_BUFFER_SIZE;

import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.privileged.event.BaseEventContext;

import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

import reactor.core.publisher.FluxSink;

/**
 * Abstract base {@link ProcessingStrategy} that creates a basic {@link Sink} that serializes events.
 */
public abstract class AbstractProcessingStrategy implements ProcessingStrategy {

  public static final String TRANSACTIONAL_ERROR_MESSAGE = "Unable to process a transactional flow asynchronously";

  public static final String PROCESSOR_SCHEDULER_CONTEXT_KEY = "mule.nb.processorScheduler";

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    return new DirectSink(pipeline, createOnEventConsumer(), SMALL_BUFFER_SIZE);
  }

  protected Consumer<CoreEvent> createOnEventConsumer() {
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
  static class ReactorSink<E> implements Sink, Disposable {

    private final FluxSink<E> fluxSink;
    private final reactor.core.Disposable disposable;
    private final Consumer onEventConsumer;
    private final int bufferSize;

    ReactorSink(FluxSink<E> fluxSink, reactor.core.Disposable disposable,
                Consumer<CoreEvent> onEventConsumer, int bufferSize) {
      this.fluxSink = fluxSink;
      this.disposable = disposable;
      this.onEventConsumer = onEventConsumer;
      this.bufferSize = bufferSize;
    }

    @Override
    public final void accept(CoreEvent event) {
      onEventConsumer.accept(event);
      fluxSink.next(intoSink(event));
    }

    @Override
    public final boolean emit(CoreEvent event) {
      onEventConsumer.accept(event);
      // Optimization to avoid using synchronized block for all emissions.
      // See: https://github.com/reactor/reactor-core/issues/1037
      long remainingCapacity = fluxSink.requestedFromDownstream();
      if (remainingCapacity == 0) {
        return false;
      } else if (remainingCapacity > (bufferSize > CORES * 4 ? CORES : 0)) {
        // If there is sufficient room in buffer to significantly reduce change of concurrent emission when buffer is full then
        // emit without synchronized block.
        fluxSink.next(intoSink(event));
        return true;
      } else {
        // If there is very little room in buffer also emit but synchronized.
        synchronized (fluxSink) {
          if (remainingCapacity > 0) {
            fluxSink.next(intoSink(event));
            return true;
          } else {
            return false;
          }
        }
      }
    }

    protected E intoSink(CoreEvent event) {
      return (E) event;
    }

    @Override
    public final void dispose() {
      fluxSink.complete();
      disposable.dispose();
    }

  }
}
