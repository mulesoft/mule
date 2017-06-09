/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.scheduler.SchedulerService;

import java.util.function.Supplier;

/**
 * Creates {@link ReactorProcessingStrategy} instance that use the {@link SchedulerService#cpuLightScheduler()} to process all
 * incoming events. In contrast to the {@link ReactorProcessingStrategy} the proactor pattern treats
 * {@link ProcessingType#CPU_INTENSIVE} and {@link ProcessingType#BLOCKING} processors differently and schedules there execution
 * on dedicated {@link SchedulerService#cpuIntensiveScheduler()} and {@link SchedulerService#ioScheduler()} ()} schedulers.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 * <p/>
 * NOTE: This processing strategy currently has a major issue with functional consistency given that the OVERLOAD error that is
 * returned by the Flow when using this strategy when an IO Scheduler has no free threads can occur at any point in the Flow
 * rather than just at the start. The implication of this behaviour is that:
 * <ul>
 * <li>As an OVERLOAD situation can occur at any point in the Flow potentially this can result in partly-processed requests.</li>
 * <li>If a source or client retries a request that previously resulted in OVERLOAD then part of the Flow may be processed twice
 * for the same message.</li>
 * </ul>
 *
 * @since 4.0
 */
public class ProactorProcessingStrategyFactory extends ReactorProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new ProactorProcessingStrategy(() -> muleContext.getSchedulerService()
        .cpuLightScheduler(muleContext.getSchedulerBaseConfig().withName(schedulersNamePrefix + "." + CPU_LITE.name())
            .withMaxConcurrentTasks(getMaxConcurrency())),
                                          () -> muleContext.getSchedulerService()
                                              .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                  .withName(schedulersNamePrefix + "." + BLOCKING.name())
                                                  .withMaxConcurrentTasks(getMaxConcurrency())),
                                          () -> muleContext.getSchedulerService()
                                              .cpuIntensiveScheduler(muleContext.getSchedulerBaseConfig()
                                                  .withName(schedulersNamePrefix + "." + CPU_INTENSIVE.name())
                                                  .withMaxConcurrentTasks(getMaxConcurrency())));
  }

  static class ProactorProcessingStrategy extends ReactorProcessingStrategy implements Startable, Stoppable {

    private final Supplier<Scheduler> blockingSchedulerSupplier;
    private final Supplier<Scheduler> cpuIntensiveSchedulerSupplier;
    private Scheduler blockingScheduler;
    private Scheduler cpuIntensiveScheduler;

    public ProactorProcessingStrategy(Supplier<Scheduler> cpuLightSchedulerSupplier,
                                      Supplier<Scheduler> blockingSchedulerSupplier,
                                      Supplier<Scheduler> cpuIntensiveSchedulerSupplier) {
      super(cpuLightSchedulerSupplier);
      this.blockingSchedulerSupplier = requireNonNull(blockingSchedulerSupplier);
      this.cpuIntensiveSchedulerSupplier = requireNonNull(cpuIntensiveSchedulerSupplier);
    }

    @Override
    public void start() throws MuleException {
      super.start();
      this.blockingScheduler = blockingSchedulerSupplier.get();
      this.cpuIntensiveScheduler = cpuIntensiveSchedulerSupplier.get();
    }

    @Override
    public void stop() throws MuleException {
      if (blockingScheduler != null) {
        blockingScheduler.stop();
      }
      if (cpuIntensiveScheduler != null) {
        cpuIntensiveScheduler.stop();
      }
      super.stop();
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (processor.getProcessingType() == BLOCKING) {
        return proactor(processor, blockingScheduler);
      } else if (processor.getProcessingType() == CPU_INTENSIVE) {
        return proactor(processor, cpuIntensiveScheduler);
      } else {
        return super.onProcessor(processor);
      }
    }

    private ReactiveProcessor proactor(ReactiveProcessor processor, Scheduler scheduler) {
      return publisher -> from(publisher).publishOn(fromExecutorService(decorateScheduler(scheduler)))
          .transform(processor).publishOn(fromExecutorService(decorateScheduler(getCpuLightScheduler())));
    }

    public Scheduler getBlockingScheduler() {
      return blockingScheduler;
    }

    public Scheduler getCpuIntensiveScheduler() {
      return cpuIntensiveScheduler;
    }
  }

}
