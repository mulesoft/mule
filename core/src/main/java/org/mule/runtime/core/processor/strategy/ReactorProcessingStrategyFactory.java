/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.scheduler.SchedulerConfig.config;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;

/**
 * Creates {@link ReactorProcessingStrategy} instances. This processing strategy demultiplexes incoming messages to
 * single-threaded event-loop.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class ReactorProcessingStrategyFactory implements ProcessingStrategyFactory {

  private static int DEFAULT_QUEUE_SIZE = 256;

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    // TODO MULE-11132 Use cpuLight scheduler with single-thread affinity.
    return new ReactorProcessingStrategy(() -> muleContext.getSchedulerService().cpuLightScheduler(),
                                         scheduler -> scheduler.stop(muleContext.getConfiguration().getShutdownTimeout(),
                                                                     MILLISECONDS),
                                         muleContext);
  }

  static class ReactorProcessingStrategy extends AbstractSchedulingProcessingStrategy {

    private Supplier<Scheduler> cpuLightSchedulerSupplier;
    protected Scheduler cpuLightScheduler;

    public ReactorProcessingStrategy(Supplier<Scheduler> cpuLightSchedulerSupplier,
                                     Consumer<Scheduler> schedulerStopper,
                                     MuleContext muleContext) {
      super(schedulerStopper, muleContext);
      this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
    }

    @Override
    public void start() throws MuleException {
      this.cpuLightScheduler = cpuLightSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (cpuLightScheduler != null) {
        getSchedulerStopper().accept(cpuLightScheduler);
      }
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onPipeline(FlowConstruct flowConstruct,
                                                                   Function<Publisher<Event>, Publisher<Event>> pipelineFunction,
                                                                   MessagingExceptionHandler messagingExceptionHandler) {
      return publisher -> from(publisher)
          .publishOn(fromExecutorService(getExecutorService(cpuLightScheduler)))
          .transform(pipelineFunction);
    }

  }

}
