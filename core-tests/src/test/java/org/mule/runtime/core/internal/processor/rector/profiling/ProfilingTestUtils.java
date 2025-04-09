/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.rector.profiling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.feature.internal.config.profiling.ProfilingFeatureFlaggingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Utils for testing profiling.
 *
 * @since 4.5.0
 */
public class ProfilingTestUtils {

  public static final String TEST_DATA_CONSUMER = "TEST_DATA_CONSUMER";

  private ProfilingTestUtils() {};

  /**
   * mocks the return of the transformation of a reactive chain for the processing strategy, triggering a profiling event.
   *
   * @param coreProfilingService  the core profiling service to mock
   * @param profilingDataProducer the profiling service data producer
   * @see InternalProfilingService
   */
  public static void mockProcessingStrategyProfilingChain(InternalProfilingService coreProfilingService,
                                                          ProfilingDataProducer profilingDataProducer) {
    when(coreProfilingService.enrichWithProfilingEventFlux(any(), any(), any()))
        .thenAnswer(i -> ((Flux<CoreEvent>) i.getArgument(0)).doOnNext(e -> profilingDataProducer.triggerProfilingEvent(null)));

    when(coreProfilingService.enrichWithProfilingEventMono(any(), any(), any()))
        .thenAnswer(i -> ((Mono<CoreEvent>) i.getArgument(0)).doOnNext(e -> profilingDataProducer.triggerProfilingEvent(null)));

    when(coreProfilingService.setCurrentExecutionContext(any(Flux.class), any()))
        .thenAnswer(i -> i.getArgument(0));

    when(coreProfilingService.setCurrentExecutionContext(any(Mono.class), any()))
        .thenAnswer(i -> i.getArgument(0));
  }

  /**
   * mocks the return of the transformation of a reactive chain for the processing strategy, without triggering a profiling event.
   *
   * @param internalProfilingService the core profiling service to mock
   * @see InternalProfilingService
   */
  public static void mockProcessingStrategyProfilingChainWithoutTriggeringEvent(
                                                                                InternalProfilingService internalProfilingService) {
    when(internalProfilingService.enrichWithProfilingEventFlux(any(), any(), any()))
        .thenAnswer(i -> ((Flux<CoreEvent>) i.getArgument(0)));

    when(internalProfilingService.enrichWithProfilingEventMono(any(), any(), any()))
        .thenAnswer(i -> ((Mono<CoreEvent>) i.getArgument(0)));

    when(internalProfilingService.setCurrentExecutionContext(any(Flux.class), any()))
        .thenAnswer(i -> i.getArgument(0));

    when(internalProfilingService.setCurrentExecutionContext(any(Mono.class), any()))
        .thenAnswer(i -> i.getArgument(0));
  }

  /**
   * @param muleContext        the muleContext where the profiling service is retrieved from
   * @param profilingEventType the {@link org.mule.test.allure.AllureConstants.Profiling}
   * @param status             the status to set
   */
  public static void enableProfilingFeatureTestConsumer(MuleContext muleContext, ProfilingEventType<?> profilingEventType,
                                                        boolean status)
      throws RegistrationException {
    ((MuleContextWithRegistry) muleContext)
        .getRegistry()
        .lookupObject(ProfilingFeatureFlaggingService.class)
        .toggleProfilingFeature(profilingEventType, TEST_DATA_CONSUMER, status);

  }
}
