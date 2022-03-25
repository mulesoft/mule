/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_PROFILING_SERVICE;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.profiling.CoreProfilingService;

import java.util.function.Function;

import javax.inject.Inject;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A {@link CoreProfilingService} that may not produce profiling data if the profiling functionality is totally disabled.
 *
 * @see DefaultProfilingService
 * @see NoOpProfilingService
 */
public class ProfilingServiceWrapper implements CoreProfilingService, Lifecycle {

  @Inject
  MuleContext muleContext;

  CoreProfilingService profilingService;

  @Inject
  FeatureFlaggingService featureFlaggingService;

  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType) {
    return getProfilingDataProducer().getProfilingDataProducer(profilingEventType);
  }

  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType,
                                                                                                   ProfilingProducerScope producerScope) {
    return getProfilingDataProducer().getProfilingDataProducer(profilingEventType, producerScope);
  }

  @Override
  public <T extends ProfilingEventContext, S> void registerProfilingDataProducer(ProfilingEventType<T> profilingEventType,
                                                                                 ProfilingDataProducer<T, S> profilingDataProducer) {
    getProfilingDataProducer().registerProfilingDataProducer(profilingEventType, profilingDataProducer);
  }

  @Override
  public <T extends ProfilingEventContext> void registerProfilingDataConsumer(ProfilingDataConsumer<T> profilingDataConsumer) {
    getProfilingDataProducer().registerProfilingDataConsumer(profilingDataConsumer);
  }

  @Override
  public ThreadSnapshotCollector getThreadSnapshotCollector() {
    return getProfilingDataProducer().getThreadSnapshotCollector();
  }

  public CoreProfilingService getProfilingDataProducer() throws MuleRuntimeException {
    if (profilingService != null) {
      return profilingService;
    }
    return initialiseProfilingService();
  }

  private CoreProfilingService initialiseProfilingService() throws MuleRuntimeException {
    if (featureFlaggingService.isEnabled(ENABLE_PROFILING_SERVICE)) {
      profilingService = new DefaultProfilingService();
    } else {
      profilingService = new NoOpProfilingService();
    }

    try {
      muleContext.getInjector().inject(profilingService);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }

    return profilingService;
  }

  @Override
  public <T extends ProfilingEventContext, S> Mono<S> enrichWithProfilingEventMono(Mono<S> original,
                                                                                   ProfilingDataProducer<T, S> dataProducer,
                                                                                   Function<S, T> transformer) {
    return profilingService.enrichWithProfilingEventMono(original, dataProducer, transformer);
  }

  @Override
  public <T extends ProfilingEventContext, S> Flux<S> enrichWithProfilingEventFlux(Flux<S> original,
                                                                                   ProfilingDataProducer<T, S> dataProducer,
                                                                                   Function<S, T> transformer) {
    return profilingService.enrichWithProfilingEventFlux(original, dataProducer, transformer);
  }

  @Override
  public void dispose() {
    if (profilingService == null) {
      initialiseProfilingService();
    }

    if (profilingService instanceof Disposable) {
      ((Disposable) profilingService).dispose();
    }
  }

  @Override
  public void initialise() throws InitialisationException {
    if (profilingService == null) {
      initialiseProfilingService();
    }

    if (profilingService instanceof Initialisable) {
      ((Initialisable) profilingService).initialise();
    }
  }

  @Override
  public void start() throws MuleException {
    if (profilingService == null) {
      initialiseProfilingService();
    }

    if (profilingService instanceof Startable) {
      ((Startable) profilingService).start();
    }
  }

  @Override
  public void stop() throws MuleException {
    if (profilingService == null) {
      initialiseProfilingService();
    }

    if (profilingService instanceof Stoppable) {
      ((Stoppable) profilingService).stop();
    }
  }
}
