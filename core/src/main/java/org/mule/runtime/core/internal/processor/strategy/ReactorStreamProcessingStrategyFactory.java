/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;

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

  protected static final LoadingCache<MuleContext, Scheduler> RETRY_SUPPORT_SCHEDULER_PROVIDER =
      newBuilder().weakKeys()
          .removalListener((MuleContext key, Scheduler value, RemovalCause cause) -> value.stop())
          .<MuleContext, Scheduler>build(mCtx -> {
            Scheduler retrySupportScheduler = mCtx.getSchedulerService()
                .customScheduler(mCtx.getSchedulerBaseConfig()
                    .withName("retrySupport")
                    .withMaxConcurrentTasks(1));

            return retrySupportScheduler;
          });

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new ReactorStreamProcessingStrategy(getRingBufferSchedulerSupplier(muleContext, schedulersNamePrefix),
                                               getBufferSize(), getSubscriberCount(),
                                               getWaitStrategy(),
                                               getCpuLightSchedulerSupplier(muleContext, schedulersNamePrefix),
                                               resolveParallelism(),
                                               getMaxConcurrency(),
                                               isMaxConcurrencyEagerCheck());
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

  static class ReactorStreamProcessingStrategy extends AbstractStreamProcessingStrategy implements Startable, Stoppable {

    private final Supplier<Scheduler> cpuLightSchedulerSupplier;
    private Scheduler cpuLightScheduler;
    private final int parallelism;

    ReactorStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize, int subscribers,
                                    String waitStrategy, Supplier<Scheduler> cpuLightSchedulerSupplier, int parallelism,
                                    int maxConcurrency, boolean maxConcurrencyEagerCheck) {
      super(ringBufferSchedulerSupplier, bufferSize, subscribers, waitStrategy, maxConcurrency, maxConcurrencyEagerCheck);
      this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
      this.parallelism = parallelism;
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      reactor.core.scheduler.Scheduler scheduler = fromExecutorService(decorateScheduler(getCpuLightScheduler()));
      if (maxConcurrency > subscribers) {
        return publisher -> {
          return from(publisher).parallel(parallelism)
              .runOn(scheduler)
              .composeGroup(super.onPipeline(pipeline));
        };
      } else {
        return super.onPipeline(pipeline);
      }
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      reactor.core.scheduler.Scheduler scheduler = fromExecutorService(decorateScheduler(getCpuLightScheduler()));
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> {
          return from(publisher).transform(processor)
              .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, getCpuLightScheduler()))
              .parallel(parallelism)
              .runOn(scheduler);
        };
      } else {
        return publisher -> from(publisher)
            .transform(processor)
            .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, getCpuLightScheduler()));
      }
    }

    protected int getParallelism() {
      return parallelism;
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
