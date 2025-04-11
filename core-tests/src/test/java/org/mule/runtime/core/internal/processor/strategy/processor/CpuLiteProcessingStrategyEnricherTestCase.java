/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.processor;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.internal.processor.rector.profiling.ProfilingTestUtils.mockProcessingStrategyProfilingChainWithoutTriggeringEvent;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.ENRICHER;

import org.junit.Before;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.enricher.AbstractEnrichedReactiveProcessorTestCase;
import org.mule.runtime.core.internal.processor.strategy.enricher.CpuLiteAsyncNonBlockingProcessingStrategyEnricher;
import org.mule.runtime.core.internal.processor.strategy.enricher.CpuLiteNonBlockingProcessingStrategyEnricher;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.concurrent.Callable;

@Feature(PROCESSING_STRATEGIES)
@Story(ENRICHER)
public class CpuLiteProcessingStrategyEnricherTestCase extends AbstractEnrichedReactiveProcessorTestCase {

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Spy
  private ImmediateScheduler scheduler;

  @Mock
  private CoreEvent coreEvent;

  @Mock
  private InternalProfilingService profilingService;

  @Before
  public void before() {
    mockProcessingStrategyProfilingChainWithoutTriggeringEvent(profilingService);
  }

  @Test
  @Description("Verify that the reactive processor is enriched in a correct way when enriched with CPU_LITE enricher")
  public void cpuLiteEnricher() {
    ReactiveProcessor transform =
        new CpuLiteNonBlockingProcessingStrategyEnricher(() -> scheduler, profilingService, ARTIFACT_ID, ARTIFACT_TYPE)
            .enrich(reactiveProcessor);

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    // For CPU LITE no thread switch should occur. The scheduler is only used for context.
    verify(scheduler, never()).submit(any(Callable.class));
  }

  @Test
  @Description("Verify that the reactive processor is enriched in a correct way when enriched with CPU_LITE_ASYNC enricher")
  public void cpuLiteAsyncEnricher() {
    ReactiveProcessor transform =
        new CpuLiteAsyncNonBlockingProcessingStrategyEnricher(() -> scheduler, () -> scheduler, profilingService, ARTIFACT_ID,
                                                              ARTIFACT_TYPE)
            .enrich(reactiveProcessor);

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(scheduler, atLeastOnce()).submit(any(Callable.class));
  }

}
