/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.api.scheduler.SchedulerService;

import java.util.function.Supplier;

/**
 * Creates {@link ReactorProcessingStrategy} instance that use the {@link SchedulerService#cpuLightScheduler()} to process all
 * incoming events.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class ReactorProcessingStrategyFactory extends AbstractProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new ReactorProcessingStrategy(() -> muleContext.getSchedulerService()
        .cpuLightScheduler(createSchedulerConfig(muleContext, schedulersNamePrefix, CPU_LITE)));
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return ReactorProcessingStrategy.class;
  }

  static class ReactorProcessingStrategy extends AbstractProcessingStrategy implements Startable, Stoppable {

    private final Supplier<Scheduler> cpuLightSchedulerSupplier;
    private Scheduler cpuLightScheduler;

    public ReactorProcessingStrategy(Supplier<Scheduler> cpuLightSchedulerSupplier) {
      this.cpuLightSchedulerSupplier = requireNonNull(cpuLightSchedulerSupplier);
    }

    @Override
    public void start() throws MuleException {
      this.cpuLightScheduler = cpuLightSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (cpuLightScheduler != null) {
        cpuLightScheduler.stop();
      }
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      return new StreamPerEventSink(pipeline, createOnEventConsumer());
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      return publisher -> from(publisher).publishOn(fromExecutorService(decorateScheduler(cpuLightScheduler)))
          .transform(pipeline);
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> from(publisher).transform(processor)
            .publishOn(fromExecutorService(decorateScheduler(cpuLightScheduler)));
      } else {
        return super.onProcessor(processor);
      }
    }

    protected Scheduler getCpuLightScheduler() {
      return cpuLightScheduler;
    }

  }

}
