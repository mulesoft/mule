/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Math.min;
import static java.lang.Runtime.getRuntime;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.api.scheduler.SchedulerService;

import java.util.function.Supplier;


/**
 * Creates {@link ReactorStreamProcessingStrategyFactory} instances that implements the reactor pattern by de-multiplexes incoming
 * messages onto a single event-loop using a ring-buffer and then using using the {@link SchedulerService#cpuLightScheduler()} to
 * process events from the ring-buffer.
 * <p>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class ReactorStreamProcessingStrategyFactory extends AbstractStreamProcessingStrategyFactory {


  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new ReactorStreamProcessingStrategy(() -> muleContext.getSchedulerService()
        .customScheduler(muleContext.getSchedulerBaseConfig()
            .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
            .withMaxConcurrentTasks(getSubscriberCount() + 1).withWaitAllowed(true)), getBufferSize(), getSubscriberCount(),
                                               getWaitStrategy(),
                                               () -> muleContext.getSchedulerService()
                                                   .cpuLightScheduler(muleContext.getSchedulerBaseConfig()
                                                       .withName(schedulersNamePrefix + "." + CPU_LITE.name())),
                                               getMaxConcurrency());
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return ReactorStreamProcessingStrategy.class;
  }

  static class ReactorStreamProcessingStrategy extends AbstractStreamProcessingStrategy implements Startable, Stoppable {

    private Supplier<Scheduler> cpuLightSchedulerSupplier;
    private Scheduler cpuLightScheduler;

    ReactorStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize, int subscribers,
                                    String waitStrategy, Supplier<Scheduler> cpuLightSchedulerSupplier, int maxConcurrency) {
      super(ringBufferSchedulerSupplier, bufferSize, subscribers, waitStrategy, maxConcurrency);
      this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      if (maxConcurrency > subscribers) {
        return publisher -> from(publisher).parallel(getNumCpuLightThreads())
            .runOn(fromExecutorService(decorateScheduler(getCpuLightScheduler())))
            .composeGroup(pipeline);
      } else {
        return super.onPipeline(pipeline);
      }
    }

    private int getNumCpuLightThreads() {
      return min(getRuntime().availableProcessors() * 2, maxConcurrency);
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> from(publisher).transform(processor).parallel(getNumCpuLightThreads())
            .runOn(fromExecutorService(decorateScheduler(getCpuLightScheduler())));
      } else {
        return super.onProcessor(processor);
      }
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

    protected Scheduler getCpuLightScheduler() {
      return cpuLightScheduler;
    }
  }

}
