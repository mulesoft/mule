/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static java.lang.Thread.currentThread;
import static org.mockito.Answers.RETURNS_MOCKS;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.FLOW_EXECUTED;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.api.profiling.type.RuntimeProfilingEventTypes.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.core.internal.processor.rector.profiling.ProfilingTestUtils.mockProcessingStrategyProfilingChain;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.PipelineProcessingStrategyReactiveProcessorBuilder.pipelineProcessingStrategyReactiveProcessorFrom;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.enricher.AbstractEnrichedReactiveProcessorTestCase;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;

import java.util.concurrent.Callable;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Feature(PROCESSING_STRATEGIES)
@Story(REACTOR)
public class PipelineProcessingStrategyReactiveProcessorBuilderTestCase extends AbstractEnrichedReactiveProcessorTestCase {

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Spy
  private ImmediateScheduler flowDispatcherScheduler;

  @Mock
  private CoreEvent coreEvent;

  @Mock(answer = RETURNS_MOCKS)
  private InternalProfilingService profilingService;

  @Mock
  private ProfilingDataProducer profilingDataProducer;

  @Before
  public void before() {
    mockProcessingStrategyProfilingChain(profilingService, profilingDataProducer);
  }


  @Test
  @Description("The reactive processor created by the builder uses the set scheduler and emits the core events")
  public void componentReactiveProcessorBuilt() {
    ReactiveProcessor transform =
        pipelineProcessingStrategyReactiveProcessorFrom(reactiveProcessor, currentThread().getContextClassLoader(),
                                                        ARTIFACT_ID, ARTIFACT_TYPE)
            .withScheduler(flowDispatcherScheduler)
            .withProfilingService(profilingService)
            .build();

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(flowDispatcherScheduler, atLeastOnce()).submit(any(Callable.class));

    // The profiling data producers are obtained
    verify(profilingService, atLeastOnce()).getProfilingDataProducer(PS_SCHEDULING_FLOW_EXECUTION);
    verify(profilingService, atLeastOnce()).getProfilingDataProducer(STARTING_FLOW_EXECUTION);
    verify(profilingService, atLeastOnce()).getProfilingDataProducer(FLOW_EXECUTED);
    verify(profilingDataProducer, atLeastOnce()).triggerProfilingEvent(any());
  }
}
