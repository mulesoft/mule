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
import static org.mule.runtime.core.internal.processor.strategy.reactor.builder.PipelineProcessingStrategyReactiveProcessorBuilder.pipelineProcessingStrategyReactiveProcessorFrom;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.PROCESSING_STRATEGIES;
import static org.mule.test.allure.AllureConstants.ProcessingStrategiesFeature.ProcessingStrategiesStory.REACTOR;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.strategy.enricher.AbstractEnrichedReactiveProcessorTestCase;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;

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
@Story(REACTOR)
public class PipelineProcessingStrategyReactiveProcessorBuilderTestCase extends AbstractEnrichedReactiveProcessorTestCase {

  private final ReactiveProcessor reactiveProcessor = p -> p;

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Spy
  private ImmediateScheduler flowDispatcherScheduler;

  @Mock
  private CoreEvent coreEvent;

  @Test
  @Description("The reactive processor created by the builder uses the set scheduler and emits the core events")
  public void componentReactiveProcessorBuilt() {
    ReactiveProcessor transform =
        pipelineProcessingStrategyReactiveProcessorFrom(reactiveProcessor, Thread.currentThread().getContextClassLoader())
            .withScheduler(flowDispatcherScheduler)
            .withSchedulerDecorator(identity())
            .build();

    createAndExecuteEnrichedTransformer(transform, coreEvent);

    verify(flowDispatcherScheduler, atLeastOnce()).submit(any(Callable.class));
  }
}
