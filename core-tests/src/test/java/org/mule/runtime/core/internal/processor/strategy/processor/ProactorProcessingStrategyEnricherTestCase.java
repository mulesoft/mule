/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.processor;

import static java.util.Arrays.asList;
import static java.util.function.Function.identity;
import static org.junit.runners.Parameterized.Parameters;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.internal.processor.rector.profiling.ProfilingTestUtils.mockProcessingStrategyProfilingChainWithoutTriggeringEvent;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.ENRICHER;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.enricher.AbstractEnrichedReactiveProcessorTestCase;
import org.mule.runtime.core.internal.processor.strategy.enricher.ProactorProcessingStrategyEnricher;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;

import io.qameta.allure.Description;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collection;
import java.util.concurrent.Callable;

@Feature(PROCESSING_STRATEGIES)
@Story(ENRICHER)
@RunWith(Parameterized.class)
public class ProactorProcessingStrategyEnricherTestCase extends AbstractEnrichedReactiveProcessorTestCase {

  private final int parallelism;

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();
  @Spy
  private ImmediateScheduler dispatcherScheduler;
  @Mock
  private CoreEvent coreEvent;
  @Mock(answer = RETURNS_MOCKS)
  private InternalProfilingService profilingService;

  @Before
  public void before() {
    mockProcessingStrategyProfilingChainWithoutTriggeringEvent(profilingService);
  }

  public ProactorProcessingStrategyEnricherTestCase(int parallelism) {
    this.parallelism = parallelism;
  }

  @Parameters(name = "parallelism: {0}")
  public static Collection<Integer> paralellism() {
    return asList(1, 4);
  }

  @Test
  @Description("Verify that the reactive processor is enriched in a correct way when enriched with proactor enricher")
  public void proactorEnricher() {
    ReactiveProcessor transform =
        new ProactorProcessingStrategyEnricher(() -> dispatcherScheduler, identity(), profilingService,
                                               ARTIFACT_ID, ARTIFACT_TYPE, parallelism,
                                               parallelism, parallelism)
            .enrich(reactiveProcessor);

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(dispatcherScheduler, atLeastOnce()).submit(ArgumentMatchers.any(Callable.class));
  }
}
