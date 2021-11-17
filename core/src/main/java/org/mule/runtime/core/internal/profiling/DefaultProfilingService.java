/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling;

import static java.util.Optional.empty;
import static org.mule.runtime.core.internal.processor.strategy.util.ProfilingUtils.getArtifactId;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.threading.ThreadSnapshotCollector;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.internal.profiling.discovery.CompositeProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.core.internal.profiling.discovery.DefaultProfilingDataConsumerDiscoveryStrategy;
import org.mule.runtime.core.internal.profiling.producer.provider.ProfilingDataProducerResolver;
import org.mule.runtime.core.internal.profiling.threading.JvmThreadSnapshotCollector;
import org.mule.runtime.feature.internal.config.profiling.RuntimeFeatureFlaggingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.inject.Inject;

/**
 * Default diagnostic service for the runtime.
 * <p>
 * This is based on the Notification API.
 *
 * @since 4.4
 */
public class DefaultProfilingService extends AbstractProfilingService {

  @Inject
  private RuntimeFeatureFlaggingService featureFlaggingService;

  private Optional<Set<ProfilingDataConsumerDiscoveryStrategy>> profilingDataConsumerDiscoveryStrategies = empty();

  private ThreadSnapshotCollector threadSnapshotCollector = new JvmThreadSnapshotCollector();

  private ProfilingDataProducerResolver profilingDataProducerResolver;


  private final Map<ProfilingEventType<?>, Map<ProfilingProducerScope, ResettableProfilingDataProducer<?, ?>>> profilingDataProducers =
      new ConcurrentHashMap<>();

  @Override
  public <T extends ProfilingEventContext, S> void registerProfilingDataProducer(ProfilingEventType<T> profilingEventType,
                                                                                 ProfilingDataProducer<T, S> profilingDataProducer) {
    profilingDataProducers
        .computeIfAbsent(profilingEventType,
                         profEventType -> new ConcurrentHashMap<>())
        .put(new ArtifactProfilingProducerScope(getArtifactId(muleContext)),
             new ResettableProfilingDataProducerDelegate<T, S>(profilingDataProducer, profDataProducer -> {
             }));
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseProfilingDataProducerIfNeeded();
    super.initialise();
  }

  @Override
  protected void onDataConsumersRegistered() {
    profilingDataProducers
        .values()
        .forEach(producers -> producers.values().forEach(ResettableProfilingDataProducer::reset));
  }

  @Override
  public ThreadSnapshotCollector getThreadSnapshotCollector() {
    return threadSnapshotCollector;
  }

  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType) {
    return getProfilingDataProducer(profilingEventType,
                                    new ArtifactProfilingProducerScope(getArtifactId(muleContext)));
  }

  @Override
  public <T extends ProfilingEventContext, S> ProfilingDataProducer<T, S> getProfilingDataProducer(
                                                                                                   ProfilingEventType<T> profilingEventType,
                                                                                                   ProfilingProducerScope profilingProducerScope) {
    initialiseProfilingDataProducerIfNeeded();

    return (ProfilingDataProducer<T, S>) profilingDataProducers
        .computeIfAbsent(profilingEventType,
                         profEventType -> new ConcurrentHashMap<>())
        .computeIfAbsent(profilingProducerScope,
                         profilingProdScope -> profilingDataProducerResolver
                             .getProfilingDataProducer(profilingEventType, profilingProducerScope));
  }

  private void initialiseProfilingDataProducerIfNeeded() {
    if (profilingDataProducerResolver == null) {
      profilingDataProducerResolver = new ProfilingDataProducerResolver(this, threadSnapshotCollector, featureFlaggingService);
    }
  }

  @Override
  public ProfilingDataConsumerDiscoveryStrategy getDiscoveryStrategy() {
    Set<ProfilingDataConsumerDiscoveryStrategy> discoveryStrategies = new HashSet<>();
    discoveryStrategies.add(new DefaultProfilingDataConsumerDiscoveryStrategy());
    this.profilingDataConsumerDiscoveryStrategies.ifPresent(discoveryStrategies::addAll);
    return new CompositeProfilingDataConsumerDiscoveryStrategy(discoveryStrategies);
  }

  @Inject
  public void setProfilingDataConsumerDiscoveryStrategies(
                                                          Optional<Set<ProfilingDataConsumerDiscoveryStrategy>> profilingDataConsumerDiscoveryStrategies) {
    this.profilingDataConsumerDiscoveryStrategies = profilingDataConsumerDiscoveryStrategies;
  }

  @Override
  public <T extends ProfilingEventContext, S> Mono<S> enrichWithProfilingEventMono(Mono<S> original,
                                                                                   ProfilingDataProducer<T, S> dataProducer,
                                                                                   Function<S, T> transformer) {
    return original.doOnNext(s -> dataProducer.triggerProfilingEvent(s, transformer));
  }

  @Override
  public <T extends ProfilingEventContext, S> Flux<S> enrichWithProfilingEventFlux(Flux<S> original,
                                                                                   ProfilingDataProducer<T, S> dataProducer,
                                                                                   Function<S, T> transformer) {
    return original.doOnNext(s -> dataProducer.triggerProfilingEvent(s, transformer));
  }
}
