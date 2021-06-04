/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.pipeline.ProcessingStrategyPipelineProfiler;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import static java.lang.Thread.currentThread;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;

@Feature(PROCESSING_STRATEGIES)
@Story(REACTOR)
public class PipelineProcessingStrategyTransformerBuilderTestCase {

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Mock
  private CoreEvent coreEvent;

  @Spy
  private ImmediateScheduler flowScheduler;

  @Mock
  private ProcessingStrategyPipelineProfiler profiler;

  private ReactiveProcessor processor = p -> p;


  public PipelineProcessingStrategyTransformerBuilderTestCase() {}


  @Test
  @Description("Verifies that the profiler performs the actions after dispatching and finishing processing events")
  public void tracerMethodsAreInvokedForEachEvent() {
    TestPublisher testPublisher = TestPublisher.create();

    // We create a chain with a processor strategy.
    Flux<CoreEvent> chain = testPublisher.flux()
        .transform(processingStrategy());

    StepVerifier.create(chain)
        .then(() -> testPublisher.emit(coreEvent, coreEvent, coreEvent))
        .expectNextCount(3)
        .verifyComplete();

    verify(profiler, times(3)).profileBeforeDispatchingToPipeline(any(CoreEvent.class));
    verify(profiler, times(3)).profileAfterPipelineProcessed(any(CoreEvent.class));

  }

  private ReactiveProcessor processingStrategy() {
    return PipelineProcessingStrategyTransformerBuilder
        .buildPipelineProcessingStrategyTransformerFrom(processor, currentThread().getContextClassLoader())
        .withScheduler(flowScheduler)
        .withProfiler(profiler).build();
  }
}
