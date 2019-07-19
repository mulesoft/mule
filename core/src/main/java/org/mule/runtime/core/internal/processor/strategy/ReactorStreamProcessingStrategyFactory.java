/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.WorkQueueStreamProcessingStrategyFactory.WorkQueueStreamProcessingStrategy;

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
public class ReactorStreamProcessingStrategyFactory extends AbstractStreamWorkQueueProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new ReactorStreamProcessingStrategy(getRingBufferSchedulerSupplier(muleContext, schedulersNamePrefix),
                                               getBufferSize(), getSubscriberCount(),
                                               getWaitStrategy(),
                                               getCpuLightSchedulerSupplier(muleContext, schedulersNamePrefix),
                                               resolveParallelism(),
                                               getMaxConcurrency(),
                                               isMaxConcurrencyEagerCheck(), muleContext.getSchedulerService());
  }

  protected int resolveParallelism() {
    if (getMaxConcurrency() == Integer.MAX_VALUE) {
      return max(CORES / getSubscriberCount(), 1);
    } else {
      return min(CORES, max(getMaxConcurrency() / getSubscriberCount(), 1));
    }
  }

  protected Supplier<Scheduler> getCpuLightSchedulerSupplier(MuleContext muleContext, String schedulersNamePrefix) {
    return () -> muleContext.getSchedulerService()
        .cpuLightScheduler(muleContext.getSchedulerBaseConfig()
            .withName(schedulersNamePrefix + "." + CPU_LITE.name()));
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return ReactorStreamProcessingStrategy.class;
  }

  static class ReactorStreamProcessingStrategy extends AbstractReactorStreamProcessingStrategy implements Startable, Stoppable {

    private final WorkQueueStreamProcessingStrategy workQueueStreamProcessingStrategy;

    ReactorStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize, int subscribers,
                                    String waitStrategy, Supplier<Scheduler> cpuLightSchedulerSupplier, int parallelism,
                                    int maxConcurrency, boolean maxConcurrencyEagerCheck, SchedulerService schedulerService) {
      super(subscribers, cpuLightSchedulerSupplier, parallelism,
            maxConcurrency, maxConcurrencyEagerCheck, schedulerService);
      this.workQueueStreamProcessingStrategy = new WorkQueueStreamProcessingStrategy(ringBufferSchedulerSupplier,
                                                                                     bufferSize,
                                                                                     subscribers,
                                                                                     waitStrategy,
                                                                                     // This scheduler is not actually used by the
                                                                                     // sink that is built
                                                                                     cpuLightSchedulerSupplier,
                                                                                     maxConcurrency,
                                                                                     maxConcurrencyEagerCheck);
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      return workQueueStreamProcessingStrategy.createSink(flowConstruct, pipeline);
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      reactor.core.scheduler.Scheduler scheduler = fromExecutorService(decorateScheduler(getCpuLightScheduler()));
      if (maxConcurrency > subscribers) {
        return publisher -> from(publisher)
            .parallel(getParallelism())
            .runOn(scheduler)
            .composeGroup(super.onPipeline(pipeline));
      } else {
        return super.onPipeline(pipeline);
      }
    }

  }

}
