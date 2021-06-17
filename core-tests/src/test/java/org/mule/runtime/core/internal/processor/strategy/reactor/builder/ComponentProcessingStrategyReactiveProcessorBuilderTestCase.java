/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static java.util.Arrays.asList;
import static org.junit.runners.Parameterized.Parameters;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ComponentProcessingStrategyReactiveProcessorBuilder.processingStrategyReactiveProcessorFrom;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.enricher.AbstractEnrichedReactiveProcessorTestCase;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collection;
import java.util.concurrent.Callable;

@Feature(PROCESSING_STRATEGIES)
@Story(REACTOR)
@RunWith(Parameterized.class)
public class ComponentProcessingStrategyReactiveProcessorBuilderTestCase extends AbstractEnrichedReactiveProcessorTestCase {

  private final ReactiveProcessor reactiveProcessor = p -> p;

  private final int parallelism;

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Spy
  private ImmediateScheduler callbackScheduler;

  @Spy
  private ImmediateScheduler dispatcherScheduler;

  @Mock
  private Scheduler contextScheduler;

  @Mock
  private CoreEvent coreEvent;

  public ComponentProcessingStrategyReactiveProcessorBuilderTestCase(int parallelism) {
    this.parallelism = parallelism;
  }

  @Parameters(name = "parallelism: {0}")
  public static Collection<Integer> paralellism() {
    return asList(1, 4);
  }

  @Test
  @Description("The reactor chain created by the builder uses the set schedulers and emits the core events")
  public void pipelineReactiveProcessorBuilt() {
    ReactiveProcessor transform =
        processingStrategyReactiveProcessorFrom(reactiveProcessor, contextScheduler)
            .withDispatcherScheduler(dispatcherScheduler)
            .withCallbackScheduler(callbackScheduler)
            .withParallelism(parallelism)
            .build();

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(callbackScheduler, atLeastOnce()).submit(any(Callable.class));
    verify(dispatcherScheduler, atLeastOnce()).submit(any(Callable.class));
  }
}
