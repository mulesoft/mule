/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static java.lang.Thread.currentThread;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.FLOW_EXECUTED;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.PS_SCHEDULING_FLOW_EXECUTION;
import static org.mule.runtime.core.api.profiling.notification.RuntimeProfilingEventType.STARTING_FLOW_EXECUTION;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.PipelineProcessingStrategyReactiveProcessorBuilder.pipelineProcessingStrategyReactiveProcessorFrom;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;

import org.mule.runtime.api.profiling.ProfilingDataProducer;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.enricher.AbstractEnrichedReactiveProcessorTestCase;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;

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

  public static final String ARTIFACT_ID = "artifactId";
  public static final String ARTIFACT_TYPE = "artifactType";

  private final ReactiveProcessor reactiveProcessor = p -> p;

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Spy
  private ImmediateScheduler flowDispatcherScheduler;

  @Mock
  private CoreEvent coreEvent;

  @Mock
  private ProfilingService profilingService;

  @Mock
  private ProfilingDataProducer profilingProducer;

  @Before
  public void before() {
    when(profilingService.getProfilingDataProducer(any())).thenReturn(profilingProducer);
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
    verify(profilingProducer, atLeastOnce()).triggerProfilingEvent(any());
  }
}
