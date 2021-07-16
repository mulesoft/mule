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
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.OPERATION_EXECUTED;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_MESSAGE_PASSING;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_SCHEDULING_OPERATION_EXECUTION;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.STARTING_OPERATION_EXECUTION;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.ComponentProcessingStrategyReactiveProcessorBuilder.processingStrategyReactiveProcessorFrom;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.diagnostics.DiagnosticsService;
import org.mule.runtime.core.api.diagnostics.ProfilingDataProducer;
import org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.enricher.AbstractEnrichedReactiveProcessorTestCase;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;

import java.util.Collection;
import java.util.concurrent.Callable;

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

@Feature(PROCESSING_STRATEGIES)
@Story(REACTOR)
@RunWith(Parameterized.class)
public class ComponentProcessingStrategyReactiveProcessorBuilderTestCase extends AbstractEnrichedReactiveProcessorTestCase {

  public static final String ARTIFACT_ID = "artifactId";
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
  private MuleContext muleContext;

  @Mock
  private CoreEvent coreEvent;

  @Mock
  private MuleConfiguration configuration;

  @Mock
  private DiagnosticsService diagnosticsService;

  @Mock
  private ProfilingDataProducer profilerProducer;

  public ComponentProcessingStrategyReactiveProcessorBuilderTestCase(int parallelism) {
    this.parallelism = parallelism;
  }

  @Parameters(name = "parallelism: {0}")
  public static Collection<Integer> paralellism() {
    return asList(1, 4);
  }

  @Before
  public void before() {
    when(muleContext.getConfiguration()).thenReturn(configuration);
    when(configuration.getId()).thenReturn(ARTIFACT_ID);
    when(muleContext.getArtifactType()).thenReturn(ArtifactType.APP);
    when(diagnosticsService.getProfilingDataProducer(any())).thenReturn(profilerProducer);
  }

  @Test
  @Description("The reactor chain created by the builder uses the set schedulers and emits the core events")
  public void pipelineReactiveProcessorBuilt() {
    ReactiveProcessor transform =
        processingStrategyReactiveProcessorFrom(reactiveProcessor, contextScheduler, muleContext)
            .withDispatcherScheduler(dispatcherScheduler)
            .withCallbackScheduler(callbackScheduler)
            .withDiagnosticsService(diagnosticsService)
            .withParallelism(parallelism)
            .build();

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(callbackScheduler, atLeastOnce()).submit(any(Callable.class));
    verify(dispatcherScheduler, atLeastOnce()).submit(any(Callable.class));

    // The profiler data producers are obtained
    verify(diagnosticsService, atLeastOnce()).getProfilingDataProducer(PS_FLOW_MESSAGE_PASSING);
    verify(diagnosticsService, atLeastOnce()).getProfilingDataProducer(OPERATION_EXECUTED);
    verify(diagnosticsService, atLeastOnce()).getProfilingDataProducer(STARTING_OPERATION_EXECUTION);
    verify(diagnosticsService, atLeastOnce()).getProfilingDataProducer(PS_SCHEDULING_OPERATION_EXECUTION);
    verify(profilerProducer, atLeastOnce()).event(any());
  }
}
