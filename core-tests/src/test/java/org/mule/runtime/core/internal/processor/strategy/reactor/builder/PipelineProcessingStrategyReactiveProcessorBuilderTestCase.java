/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import static java.util.function.UnaryOperator.identity;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_DISPATCH;
import static org.mule.runtime.core.api.diagnostics.notification.RuntimeProfilingEventType.PS_FLOW_END;
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.PipelineProcessingStrategyReactiveProcessorBuilder.pipelineProcessingStrategyReactiveProcessorFrom;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.diagnostics.DiagnosticsService;
import org.mule.runtime.core.api.diagnostics.ProfilingDataProducer;
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

  private final ReactiveProcessor reactiveProcessor = p -> p;

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Spy
  private ImmediateScheduler flowDispatcherScheduler;

  @Mock
  private CoreEvent coreEvent;
  
  @Mock
  private MuleConfiguration configuration;

  @Mock
  private DiagnosticsService diagnosticsService;

  @Mock
  private ProfilingDataProducer profilerProducer;

  @Mock
  private MuleContext muleContext;

  @Before
  public void before() {
    when(muleContext.getConfiguration()).thenReturn(configuration);
    when(configuration.getId()).thenReturn("artifactId");
    when(muleContext.getArtifactType()).thenReturn(ArtifactType.APP);
    when(diagnosticsService.getProfilingDataProducer(any())).thenReturn(profilerProducer);
  }

  @Test
  @Description("The reactive processor created by the builder uses the set scheduler and emits the core events")
  public void componentReactiveProcessorBuilt() {
    ReactiveProcessor transform =
        pipelineProcessingStrategyReactiveProcessorFrom(reactiveProcessor, Thread.currentThread().getContextClassLoader(),
                                                        muleContext)
                                                            .withScheduler(flowDispatcherScheduler)
                                                            .withDiagnosticsService(diagnosticsService)
                                                            .withSchedulerDecorator(identity())
                                                            .build();

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(flowDispatcherScheduler, atLeastOnce()).submit(any(Callable.class));

    // The profiler data producers are obtained
    verify(diagnosticsService, atLeastOnce()).getProfilingDataProducer(PS_FLOW_DISPATCH);
    verify(diagnosticsService, atLeastOnce()).getProfilingDataProducer(PS_FLOW_END);
    verify(profilerProducer, atLeastOnce()).event(any());
  }
}
