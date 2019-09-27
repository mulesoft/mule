/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory.BLOCKING_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.internal.processor.strategy.TransactionAwareProcessingStrategyFactory.LAZY_TX_CHECK;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Decorates a {@link ProcessingStrategy} so that processing takes place on the current thread in the event of a transaction
 * being active.
 *
 * @since 4.3.0
 */
public class TransactionAwareStreamEmitterProcessingStrategyDecorator extends ProcessingStrategyDecorator {

  private static final Consumer<CoreEvent> EVENT_CONSUMER = event -> {
  };

  public TransactionAwareStreamEmitterProcessingStrategyDecorator(ProcessingStrategy delegate) {
    super(delegate);
    if (delegate instanceof ProcessingStrategyAdapter) {
      ProcessingStrategyAdapter adapter = (ProcessingStrategyAdapter) delegate;
      Function<ScheduledExecutorService, ScheduledExecutorService> delegateDecorator = adapter.getSchedulerDecorator();
      adapter.setSchedulerDecorator(scheduler -> new ConditionalExecutorServiceDecorator(delegateDecorator.apply(scheduler),
                                                                                         currentScheduler -> isTransactionActive()));
    }
  }

  @Override
  public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
    Sink delegateSink = delegate.createSink(flowConstruct, pipeline);
    Sink syncSink = new StreamPerThreadSink(pipeline, EVENT_CONSUMER, flowConstruct);
    return new TransactionalDelegateSink(syncSink, delegateSink);
  }

  @Override
  public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
    return !LAZY_TX_CHECK && isTransactionActive()
        ? BLOCKING_PROCESSING_STRATEGY_INSTANCE.onPipeline(pipeline)
        : delegate.onPipeline(pipeline);
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    if (LAZY_TX_CHECK) {
      // If there is a tx active, force the processing to the main thread right after the processor.
      // This is needed because non blocking processors will do a thread switch internally regardless of the presence of a tx
      // (which is ok, an http:request shouldn't bother that a previous db component opened a transaction).
      return publisher -> from(publisher)
          .flatMap(event -> just(event)
              .transform(isTransactionActive() ? BLOCKING_PROCESSING_STRATEGY_INSTANCE.onProcessor(processor) : processor));
    } else if (isTransactionActive()) {
      return BLOCKING_PROCESSING_STRATEGY_INSTANCE.onProcessor(processor);
    } else {
      return delegate.onProcessor(processor);
    }
  }
}
