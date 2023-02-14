/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Math.min;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.IO_RW;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactId;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactType;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.AsyncProcessingStrategyFactory;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.StreamEmitterProcessingStrategyFactory.StreamEmitterProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.enricher.ProactorProcessingStrategyEnricher;
import org.mule.runtime.core.internal.processor.strategy.enricher.ProcessingTypeBasedReactiveProcessorEnricher;
import org.mule.runtime.core.internal.util.rx.RetrySchedulerWrapper;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * Creates {@link AsyncProcessingStrategyFactory} instance that implements the proactor pattern by de-multiplexing incoming events
 * onto a multiple emitter using the {@link SchedulerService#cpuLightScheduler()} to process these events from each emitter. In
 * contrast to the {@link AbstractStreamProcessingStrategyFactory} the proactor pattern treats
 * {@link ReactiveProcessor.ProcessingType#CPU_INTENSIVE} and {@link ReactiveProcessor.ProcessingType#BLOCKING} processors
 * differently and schedules there execution on dedicated {@link SchedulerService#cpuIntensiveScheduler()} and
 * {@link SchedulerService#ioScheduler()} ()} schedulers.
 * <p/>
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.2.0
 */
public class ProactorStreamEmitterProcessingStrategyFactory extends AbstractStreamProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    Supplier<Scheduler> cpuLightSchedulerSupplier = getCpuLightSchedulerSupplier(muleContext, schedulersNamePrefix);
    return new ProactorStreamEmitterProcessingStrategy(getBufferSize(),
                                                       getSubscriberCount(),
                                                       cpuLightSchedulerSupplier,
                                                       cpuLightSchedulerSupplier,
                                                       () -> muleContext.getSchedulerService()
                                                           .ioScheduler(muleContext.getSchedulerBaseConfig()
                                                               .withName(
                                                                         schedulersNamePrefix + "." + BLOCKING.name())),
                                                       () -> muleContext.getSchedulerService()
                                                           .cpuIntensiveScheduler(muleContext.getSchedulerBaseConfig()
                                                               .withName(schedulersNamePrefix + "."
                                                                   + CPU_INTENSIVE.name())),
                                                       resolveParallelism(),
                                                       getMaxConcurrency(),
                                                       isMaxConcurrencyEagerCheck(),
                                                       () -> muleContext.getConfiguration().getShutdownTimeout());
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return ProactorStreamEmitterProcessingStrategy.class;
  }

  static class ProactorStreamEmitterProcessingStrategy extends StreamEmitterProcessingStrategy {

    private static final Logger LOGGER = getLogger(ProactorStreamEmitterProcessingStrategy.class);

    private final Supplier<Scheduler> blockingSchedulerSupplier;
    private final Supplier<Scheduler> cpuIntensiveSchedulerSupplier;

    private Scheduler blockingScheduler;
    private Scheduler cpuIntensiveScheduler;
    private boolean blockingSchedulerIsStopped;
    private boolean cpuIntensiveSchedulerIsStopped;

    public ProactorStreamEmitterProcessingStrategy(int bufferSize,
                                                   int subscriberCount,
                                                   Supplier<Scheduler> flowDispatchSchedulerSupplier,
                                                   Supplier<Scheduler> cpuLightSchedulerSupplier,
                                                   Supplier<Scheduler> blockingSchedulerSupplier,
                                                   Supplier<Scheduler> cpuIntensiveSchedulerSupplier,
                                                   int parallelism,
                                                   int maxConcurrency,
                                                   boolean maxConcurrencyEagerCheck,
                                                   Supplier<Long> shutdownTimeoutSupplier) {
      super(bufferSize, subscriberCount, flowDispatchSchedulerSupplier, cpuLightSchedulerSupplier, parallelism, maxConcurrency,
            maxConcurrencyEagerCheck, shutdownTimeoutSupplier);
      this.blockingSchedulerSupplier = blockingSchedulerSupplier;
      this.cpuIntensiveSchedulerSupplier = cpuIntensiveSchedulerSupplier;
    }

    @Override
    public void start() throws MuleException {
      this.blockingScheduler = blockingSchedulerSupplier.get();
      this.cpuIntensiveScheduler = cpuIntensiveSchedulerSupplier.get();
      super.start();
    }

    @Override
    protected int getSinksCount() {
      return min(maxConcurrency, CORES);
    }

    @Override
    protected Scheduler createCpuLightScheduler(Supplier<Scheduler> cpuLightSchedulerSupplier) {
      return new RetrySchedulerWrapper(super.createCpuLightScheduler(cpuLightSchedulerSupplier),
                                       SCHEDULER_BUSY_RETRY_INTERVAL_MS);
    }

    @Override
    protected boolean stopSchedulersIfNeeded() {
      if (super.stopSchedulersIfNeeded()) {
        stopScheduler(blockingScheduler, blockingSchedulerIsStopped);
        stopScheduler(cpuIntensiveScheduler, cpuIntensiveSchedulerIsStopped);
        blockingSchedulerIsStopped = true;
        cpuIntensiveSchedulerIsStopped = true;
        return true;
      }

      return false;
    }

    private void stopScheduler(Scheduler scheduler, boolean isStopped) {
      if (!isStopped) {
        scheduler.stop();
      }
    }

    @Override
    protected ProcessingTypeBasedReactiveProcessorEnricher getProcessingStrategyEnricher() {
      ProactorProcessingStrategyEnricher blockingEnricher = getEnricher(this.blockingScheduler);
      return super.getProcessingStrategyEnricher()
          .register(BLOCKING, blockingEnricher)
          .register(IO_RW, blockingEnricher)
          .register(CPU_INTENSIVE, getEnricher(cpuIntensiveScheduler));
    }

    private ProactorProcessingStrategyEnricher getEnricher(Scheduler blockingScheduler) {
      return new ProactorProcessingStrategyEnricher(() -> blockingScheduler,
                                                    getSchedulerDecorator().compose(this::getRetryScheduler),
                                                    getProfilingService(),
                                                    getArtifactId(muleContext),
                                                    getArtifactType(muleContext),
                                                    maxConcurrency,
                                                    getParallelism(),
                                                    subscribers);
    }

    @Override
    protected Scheduler getFlowDispatcherScheduler() {
      return getCpuLightScheduler();
    }

  }

}
