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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.management.execution.ExecutionOrchestrator;
import org.mule.runtime.core.internal.management.execution.ProcessingStrategyExecutionProfiler;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.util.Collection;
import java.util.concurrent.Callable;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static org.mockito.Mockito.*;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ComponentProcessingStrategyTransformerBuilder.buildProcessorChainFrom;
import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;

@Feature(PROCESSING_STRATEGIES)
@Story(REACTOR)
@RunWith(Parameterized.class)
public class ComponentProcessingStrategyTransformerBuilderTestCase {

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Mock
  private CoreEvent coreEvent1;

  @Mock
  private CoreEvent coreEvent2;

  @Mock
  private CoreEvent coreEvent3;

  @Mock
  private ProcessingStrategyExecutionProfiler tracer;

  @Mock
  private ExecutionOrchestrator executionOrchestrator;

  @Mock
  private Scheduler contextScheduler;

  @Spy
  private ImmediateScheduler callbackScheduler;

  @Spy
  private ImmediateScheduler dispatcherScheduler;


  private ReactiveProcessor processor = p -> p;

  private final int paralellism;


  public ComponentProcessingStrategyTransformerBuilderTestCase(int paralellism) {
    this.paralellism = paralellism;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Integer> paralellism() {
    return asList(1, 2, 3, 4);
  }

  @Before
  public void before() {
    when(executionOrchestrator.getDispatcherScheduler()).thenReturn(dispatcherScheduler);
    when(executionOrchestrator.getCallbackScheduler()).thenReturn(callbackScheduler);
    when(executionOrchestrator.getContextScheduler()).thenReturn(IMMEDIATE_SCHEDULER);
  }

  @Test
  @Description("Verifies that the management components are invoked for each event emitted by the tracer")
  public void tracerMethodsAreInvokedForEachEvent() {
    TestPublisher testPublisher = TestPublisher.create();

    // We create a chain with a processor strategy.
    Flux<CoreEvent> chain = testPublisher.flux()
        .transform(processingStrategy());

    StepVerifier.create(chain)
        .then(() -> testPublisher.emit(coreEvent1, coreEvent2, coreEvent3))
        .expectNextCount(3)
        .verifyComplete();

    verifyTracerInvocationFor(coreEvent1, coreEvent2, coreEvent3);

    // The schedulers should be invoked for each signal of reactor.
    verify(callbackScheduler, atLeastOnce()).submit(any(Callable.class));
    verify(dispatcherScheduler, atLeastOnce()).submit(any(Callable.class));
  }

  private void verifyTracerInvocationFor(CoreEvent... coreEvents) {
    stream(coreEvents).forEach(coreEvent -> verify(tracer).profileBeforeDispatchingToProcessor(coreEvent));
  }

  private ReactiveProcessor processingStrategy() {
    return buildProcessorChainFrom(processor, contextScheduler)
        .withExecutionOrchestrator(executionOrchestrator)
        .withExecutionProfiler(tracer)
        .withParallelism(paralellism)
        .build();
  }
}
