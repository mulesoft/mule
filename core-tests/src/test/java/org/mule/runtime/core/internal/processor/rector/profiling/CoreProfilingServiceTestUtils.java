/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.rector.profiling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.CoreProfilingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Utils for testing reactor profiling.
 */
public class CoreProfilingServiceTestUtils {

  /**
   * mocks the return of the transformation of a reactive chain for the processing strategy, triggering a profiling event.
   *
   * @param coreProfilingService  the core profiling service to mock
   * @param profilingDataProducer the profiling service data producer
   * @see CoreProfilingService
   */
  public static void mockProcessingStrategyProfilingChain(CoreProfilingService coreProfilingService,
                                                          ProfilingDataProducer profilingDataProducer) {
    when(coreProfilingService.enrichWithProfilingEventFlux(any(), any(), any()))
        .thenAnswer(i -> ((Flux<CoreEvent>) i.getArgument(0)).doOnNext(e -> profilingDataProducer.triggerProfilingEvent(null)));

    when(coreProfilingService.enrichWithProfilingEventMono(any(), any(), any()))
        .thenAnswer(i -> ((Mono<CoreEvent>) i.getArgument(0)).doOnNext(e -> profilingDataProducer.triggerProfilingEvent(null)));
  }

  /**
   * mocks the return of the transformation of a reactive chain for the processing strategy, without triggering a profiling event.
   *
   * @param coreProfilingService the core profiling service to mock
   * @see CoreProfilingService
   */
  public static void mockProcessingStrategyProfilingChainWithoutTriggeringEvent(CoreProfilingService coreProfilingService) {
    when(coreProfilingService.enrichWithProfilingEventFlux(any(), any(), any()))
        .thenAnswer(i -> ((Flux<CoreEvent>) i.getArgument(0)));

    when(coreProfilingService.enrichWithProfilingEventMono(any(), any(), any()))
        .thenAnswer(i -> ((Mono<CoreEvent>) i.getArgument(0)));
  }
}
