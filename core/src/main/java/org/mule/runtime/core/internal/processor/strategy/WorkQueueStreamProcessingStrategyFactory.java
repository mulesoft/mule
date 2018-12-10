/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Creates {@link WorkQueueStreamProcessingStrategy} instances that de-multiplexes incoming messages using a ring-buffer but
 * instead of processing events using a constrained {@link SchedulerService#cpuLightScheduler()}, or by using the proactor
 * pattern, instead simply performs all processing on a larger work queue pool using a fixed number of threads from the
 * {@link SchedulerService#ioScheduler()}.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class WorkQueueStreamProcessingStrategyFactory extends AbstractStreamProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new WorkQueueStreamProcessingStrategy(getRingBufferSchedulerSupplier(muleContext, schedulersNamePrefix),
                                                 getBufferSize(),
                                                 getSubscriberCount(),
                                                 getWaitStrategy(),
                                                 () -> muleContext.getSchedulerService()
                                                     .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                         .withName(schedulersNamePrefix + "." + BLOCKING.name())),
                                                 getMaxConcurrency(), isMaxConcurrencyEagerCheck());
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return WorkQueueStreamProcessingStrategy.class;
  }

  static class WorkQueueStreamProcessingStrategy extends AbstractStreamProcessingStrategy implements Startable, Stoppable {

    private final Supplier<Scheduler> blockingSchedulerSupplier;
    private Scheduler blockingScheduler;
    private final List<Sink> sinkList = new ArrayList<>();

    protected WorkQueueStreamProcessingStrategy(Supplier<Scheduler> ringBufferSchedulerSupplier, int bufferSize,
                                                int subscribers,
                                                String waitStrategy, Supplier<Scheduler> blockingSchedulerSupplier,
                                                int maxConcurrency, boolean maxConcurrencyEagerCheck) {
      super(ringBufferSchedulerSupplier, bufferSize, subscribers, waitStrategy, maxConcurrency, maxConcurrencyEagerCheck);
      this.blockingSchedulerSupplier = requireNonNull(blockingSchedulerSupplier);
    }

    @Override
    public ReactiveProcessor onPipeline(ReactiveProcessor pipeline) {
      if (maxConcurrency > subscribers) {
        return publisher -> from(publisher)
            .flatMap(event -> just(event).transform(pipeline)
                .subscribeOn(fromExecutorService(decorateScheduler(blockingScheduler)))
                .subscriberContext(ctx -> ctx.put(PROCESSOR_SCHEDULER_CONTEXT_KEY, blockingScheduler)),
                     maxConcurrency);
      } else {
        return super.onPipeline(pipeline);
      }
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == CPU_LITE_ASYNC) {
        return publisher -> from(publisher).transform(processor)
            .publishOn(fromExecutorService(decorateScheduler(blockingScheduler)));
      } else {
        return super.onProcessor(processor);
      }
    }

    @Override
    public void start() throws MuleException {
      this.blockingScheduler = blockingSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      sinkList.stream().filter(sink -> sink instanceof Disposable).forEach(sink -> ((Disposable) sink).dispose());
      if (blockingScheduler != null) {
        blockingScheduler.stop();
      }
    }

  }

}
