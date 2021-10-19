/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_PROFILING_SERVICE;
import static org.mule.runtime.core.api.construct.BackPressureReason.MAX_CONCURRENCY_EXCEEDED;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.slf4j.LoggerFactory.getLogger;
import static org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.DEFAULT_BUFFER_SIZE;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactId;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactType;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.construct.FromFlowRejectedExecutionException;
import org.mule.runtime.core.internal.processor.strategy.AbstractStreamProcessingStrategyFactory.AbstractStreamProcessingStrategy;
import org.mule.runtime.core.internal.processor.strategy.enricher.CpuLiteAsyncNonBlockingProcessingStrategyEnricher;
import org.mule.runtime.core.internal.processor.strategy.enricher.CpuLiteNonBlockingProcessingStrategyEnricher;
import org.mule.runtime.core.internal.processor.strategy.enricher.ReactiveProcessorEnricher;
import org.mule.runtime.core.internal.processor.strategy.enricher.ProcessingTypeBasedReactiveProcessorEnricher;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public abstract class AbstractReactorStreamProcessingStrategy extends AbstractStreamProcessingStrategy
    implements Lifecycle {

  private static final Logger LOGGER = getLogger(AbstractReactorStreamProcessingStrategy.class);

  private final Supplier<Scheduler> cpuLightSchedulerSupplier;
  private final int parallelism;
  private final AtomicInteger inFlightEvents = new AtomicInteger();
  private final BiConsumer<CoreEvent, Throwable> inFlightDecrementCallback = (e, t) -> {
    int decremented = inFlightEvents.decrementAndGet();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("decremented inFlightEvents={}", decremented);
    }
  };

  private Scheduler cpuLightScheduler;
  private ReactiveProcessorEnricher processorEnricher = null;

  @Inject
  ProfilingService profilingService;

  @Inject
  MuleContext muleContext;

  @Inject
  FeatureFlaggingService featureFlags;

  AbstractReactorStreamProcessingStrategy(int subscribers,
                                          Supplier<Scheduler> cpuLightSchedulerSupplier,
                                          int parallelism,
                                          int maxConcurrency,
                                          boolean maxConcurrencyEagerCheck) {
    super(subscribers, maxConcurrency, maxConcurrencyEagerCheck);
    this.cpuLightSchedulerSupplier = cpuLightSchedulerSupplier;
    this.parallelism = parallelism;
  }

  @Override
  public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
    return processorEnricher.enrich(processor);
  }

  protected ScheduledExecutorService getNonBlockingTaskScheduler() {
    return decorateScheduler(getCpuLightScheduler());
  }

  @Override
  public void checkBackpressureAccepting(CoreEvent event) throws RejectedExecutionException {
    final BackPressureReason reason = checkCapacity(event);
    if (reason != null) {
      throw new FromFlowRejectedExecutionException(reason);
    }
  }

  @Override
  public BackPressureReason checkBackpressureEmitting(CoreEvent event) {
    return checkCapacity(event);
  }

  /**
   * Check the capacity of the processing strategy to process events at the moment.
   *
   * @param event the event about to be processed
   * @return true if the event can be accepted for processing
   */
  protected BackPressureReason checkCapacity(CoreEvent event) {
    if (maxConcurrencyEagerCheck) {
      int incremented = inFlightEvents.incrementAndGet();
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("incremented inFlightEvents={}", incremented);
      }
      if (incremented > maxConcurrency) {
        int decremented = inFlightEvents.decrementAndGet();
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("decremented due to too large maxConcurrency={} inFlightEvents={}", maxConcurrency, decremented);
        }
        return MAX_CONCURRENCY_EXCEEDED;
      }

      // onResponse doesn't wait for child contexts to be terminated, which is handy when a child context is created (like in
      // an async, for instance)
      ((BaseEventContext) event.getContext()).onBeforeResponse(inFlightDecrementCallback);
    }

    return null;
  }

  protected int getParallelism() {
    return parallelism;
  }

  @Override
  public void initialise() throws InitialisationException {}

  @Override
  public void start() throws MuleException {
    processorEnricher = getProcessingStrategyEnricher();
  }

  protected ProcessingTypeBasedReactiveProcessorEnricher getProcessingStrategyEnricher() {

    this.cpuLightScheduler = createCpuLightScheduler(cpuLightSchedulerSupplier);

    String artifactId = getArtifactId(muleContext);
    String artifactType = getArtifactType(muleContext);

    CpuLiteNonBlockingProcessingStrategyEnricher cpuLiteEnricher =
        new CpuLiteNonBlockingProcessingStrategyEnricher(() -> cpuLightScheduler, getProfilingService(), artifactId,
                                                         artifactType);
    CpuLiteAsyncNonBlockingProcessingStrategyEnricher cpuLiteAsyncEnricher =
        new CpuLiteAsyncNonBlockingProcessingStrategyEnricher(() -> cpuLightScheduler, this::getNonBlockingTaskScheduler,
                                                              getProfilingService(), artifactId, artifactType);

    return new ProcessingTypeBasedReactiveProcessorEnricher(cpuLiteEnricher)
        .register(CPU_LITE, cpuLiteEnricher)
        .register(CPU_LITE_ASYNC, cpuLiteAsyncEnricher);
  }

  protected ProfilingService getProfilingService() {
    return featureFlags.isEnabled(ENABLE_PROFILING_SERVICE) ? profilingService : null;
  }

  @Override
  public void stop() {
    // This counter relies on BaseEventContext.onResponse() and other ProcessingStrategy could be still processing
    // child events that will be dropped because of this stop, impeding such invocation.
    inFlightEvents.getAndSet(0);
  }

  protected Scheduler createCpuLightScheduler(Supplier<Scheduler> cpuLightSchedulerSupplier) {
    return cpuLightSchedulerSupplier.get();
  }

  @Override
  public void dispose() {
    stopSchedulersIfNeeded();
  }

  /**
   * Stops all schedulers that should be stopped by this ProcessingStrategy
   *
   * @return whether or not it was needed or not
   */
  protected boolean stopSchedulersIfNeeded() {
    if (cpuLightScheduler != null) {
      cpuLightScheduler.stop();
      cpuLightScheduler = null;
    }

    return true;
  }

  protected Scheduler getCpuLightScheduler() {
    return cpuLightScheduler;
  }

  protected int getBufferQueueSize() {
    return DEFAULT_BUFFER_SIZE;
  }
}
