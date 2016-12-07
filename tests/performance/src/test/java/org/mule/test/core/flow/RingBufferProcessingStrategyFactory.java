/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.flow;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import static reactor.util.concurrent.QueueSupplier.SMALL_BUFFER_SIZE;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.scheduler.SchedulerConfig;
import org.mule.runtime.core.processor.strategy.AbstractSchedulingProcessingStrategy;

import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.TopicProcessor;
import reactor.util.concurrent.QueueSupplier;

/**
 * Creates {@link RingBufferProcessingStrategy} instances. This processing strategy demultiplexes incoming messages to
 * single-threaded event-loop.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class RingBufferProcessingStrategyFactory implements ProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String name) {
    return new RingBufferProcessingStrategy(() -> muleContext.getSchedulerService()
        .customScheduler(config().withMaxConcurrentTasks(2), SMALL_BUFFER_SIZE),
                                            scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(),
                                                                        MILLISECONDS),
                                            muleContext);
  }

  static class RingBufferProcessingStrategy extends AbstractSchedulingProcessingStrategy {

    private Supplier<Scheduler> cpuLightSchedulerSupplier;
    protected Scheduler eventLoop;

    public RingBufferProcessingStrategy(Supplier<Scheduler> cpuLightSchedulerSupplier,
                                        Consumer<Scheduler> schedulerStopper, MuleContext muleContext) {
      super(schedulerStopper, muleContext);
      this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction,
                                                                   MessagingExceptionHandler messagingExceptionHandler) {
      return publisher -> from(publisher)
          // TODO Work out why this fails with our scheduler
          .subscribeWith(TopicProcessor.create())
          .doOnNext(assertCanProcess())
          .transform(pipelineFunction);
    }

    protected Consumer<Event> assertCanProcess() {
      return event -> {
        if (isTransactionActive()) {
          throw propagate(new DefaultMuleException(createStaticMessage(TRANSACTIONAL_ERROR_MESSAGE)));
        }
      };
    }

    @Override
    public void start() throws MuleException {
      eventLoop = cpuLightSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      getSchedulerStopper().accept(eventLoop);
    }

  }

}
