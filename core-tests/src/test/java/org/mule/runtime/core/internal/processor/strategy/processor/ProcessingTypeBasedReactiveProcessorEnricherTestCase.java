/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.processor;

import static java.util.Arrays.asList;
import static java.util.function.UnaryOperator.identity;
import static org.junit.runners.Parameterized.Parameters;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.BLOCKING;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.internal.processor.rector.profiling.ProfilingTestUtils.mockProcessingStrategyProfilingChainWithoutTriggeringEvent;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.ENRICHER;

import org.junit.Before;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.enricher.AbstractEnrichedReactiveProcessorTestCase;
import org.mule.runtime.core.internal.processor.strategy.enricher.CpuLiteAsyncNonBlockingProcessingStrategyEnricher;
import org.mule.runtime.core.internal.processor.strategy.enricher.CpuLiteNonBlockingProcessingStrategyEnricher;
import org.mule.runtime.core.internal.processor.strategy.enricher.ProactorProcessingStrategyEnricher;
import org.mule.runtime.core.internal.processor.strategy.enricher.ProcessingTypeBasedReactiveProcessorEnricher;
import org.mule.runtime.core.internal.processor.strategy.enricher.ReactiveProcessorEnricher;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;
import org.mule.runtime.core.internal.profiling.ReactorAwareProfilingService;

import java.util.Collection;
import java.util.concurrent.Callable;

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
import org.reactivestreams.Publisher;

@Feature(PROCESSING_STRATEGIES)
@Story(ENRICHER)
@RunWith(Parameterized.class)
public class ProcessingTypeBasedReactiveProcessorEnricherTestCase extends AbstractEnrichedReactiveProcessorTestCase {

  public static final String ARTIFACT_ID = "artifactId";
  public static final String ARTIFACT_TYPE = "artifactType";
  private final int parallelism;
  private final ReactiveProcessor cpuLiteProcessor = p -> p;
  private final ReactiveProcessor cpuLiteAsyncReactiveProcessor = new CpuLiteAsyncReactiveProcessor();
  private final ReactiveProcessor blockingAsyncReactiveProcessor = new BlockingReactiveProcessor();

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Spy
  private ImmediateScheduler proactorScheduler;

  @Spy
  private ImmediateScheduler cpuLiteScheduler;

  @Spy
  private ImmediateScheduler cpuLiteAsyncScheduler;

  @Mock(answer = RETURNS_MOCKS)
  private ReactorAwareProfilingService profilingService;

  @Mock
  private CoreEvent coreEvent;

  @Before
  public void before() {
    mockProcessingStrategyProfilingChainWithoutTriggeringEvent(profilingService);
  }

  public ProcessingTypeBasedReactiveProcessorEnricherTestCase(int parallelism) {
    this.parallelism = parallelism;
  }

  @Parameters(name = "parallelism: {0}")
  public static Collection<Integer> paralellism() {
    return asList(1, 4);
  }

  @Test
  @Description("Verify that the reactive processor is enriched in a correct way when the reactive processor is registered to use the CpuLite enricher")
  public void processingTypeBasedEnricherUsingCpuLiteEnricher() {
    ReactiveProcessor transform = getProcessingTypeBasedEnricher().enrich(cpuLiteProcessor);

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(cpuLiteScheduler, never()).submit(any(Callable.class));
    verify(cpuLiteAsyncScheduler, never()).submit(any(Callable.class));
    verify(proactorScheduler, never()).submit(any(Callable.class));
  }

  @Test
  @Description("Verify that the reactive processor is enriched in a correct way when the reactive processor is registered to use the CpuLiteAsync enricher")
  public void processingTypeBasedEnricherUsingCpuLiteAsyncEnricher() {
    ReactiveProcessor transform = getProcessingTypeBasedEnricher().enrich(cpuLiteAsyncReactiveProcessor);

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(cpuLiteScheduler, never()).submit(any(Callable.class));
    verify(cpuLiteAsyncScheduler, atLeastOnce()).submit(any(Callable.class));
    verify(proactorScheduler, never()).submit(any(Callable.class));
  }

  @Test
  @Description("Verify that the reactive processor is enriched in a correct way when the reactive processor is registered to use the proactor enricher")
  public void processingTypeBasedEnricherUsingProactorEnricher() {
    ReactiveProcessor transform = getProcessingTypeBasedEnricher().enrich(blockingAsyncReactiveProcessor);

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(cpuLiteScheduler, never()).submit(any(Callable.class));
    verify(cpuLiteAsyncScheduler, never()).submit(any(Callable.class));
    verify(proactorScheduler, atLeastOnce()).submit(any(Callable.class));
  }

  private ReactiveProcessorEnricher getProcessingTypeBasedEnricher() {

    ReactiveProcessorEnricher proactorEnricher =
        new ProactorProcessingStrategyEnricher(() -> proactorScheduler, identity(), profilingService, ARTIFACT_ID, ARTIFACT_TYPE,
                                               parallelism,
                                               parallelism,
                                               parallelism);

    ReactiveProcessorEnricher cpuLiteEnricher =
        new CpuLiteNonBlockingProcessingStrategyEnricher(() -> cpuLiteScheduler, profilingService, ARTIFACT_ID, ARTIFACT_TYPE);

    ReactiveProcessorEnricher cpuLiteAsyncEnricher =
        new CpuLiteAsyncNonBlockingProcessingStrategyEnricher(() -> cpuLiteAsyncScheduler, () -> cpuLiteAsyncScheduler,
                                                              profilingService, ARTIFACT_ID, ARTIFACT_TYPE);

    return new ProcessingTypeBasedReactiveProcessorEnricher(cpuLiteEnricher)
        .register(CPU_LITE, cpuLiteEnricher)
        .register(CPU_LITE_ASYNC, cpuLiteAsyncEnricher)
        .register(BLOCKING, proactorEnricher);
  }

  /**
   * Test {@link ReactiveProcessor}.
   */
  private static class TestReactiveProcessor implements ReactiveProcessor {

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> coreEventPublisher) {
      return coreEventPublisher;
    }

  }

  /**
   * Test BLOCKING {@link ReactiveProcessor}.
   */
  private static class BlockingReactiveProcessor extends TestReactiveProcessor {

    @Override
    public ProcessingType getProcessingType() {
      return BLOCKING;
    }

  }

  /**
   * Test CPU LITE ASYNC {@link ReactiveProcessor}.
   */
  private static class CpuLiteAsyncReactiveProcessor extends TestReactiveProcessor {

    @Override
    public ProcessingType getProcessingType() {
      return CPU_LITE_ASYNC;
    }

  }
}
