/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.execution;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.mule.runtime.core.internal.management.execution.LoggerProcessingStrategyExecutionProfiler.*;
import static org.mule.test.allure.AllureConstants.ManagementFeature.MANAGEMENT;
import static org.mule.test.allure.AllureConstants.ManagementFeature.ProcessingStrategyExecutionProfiler.LOGGER_PROCESSING_STRATEGY_EXECUTION_PROFILER;

@Feature(MANAGEMENT)
@Story(LOGGER_PROCESSING_STRATEGY_EXECUTION_PROFILER)
public class LoggerProcessingStrategyExecutionProfilerTetCase {

  public static final String CORRELATION_ID = "correlationId";
  private static final String LOCATION = "/location";

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Mock
  private ReactiveProcessor processor;

  @Mock
  private CoreEvent event;

  private LoggerProcessingStrategyExecutionProfiler streamInterceptor;

  @Before
  public void before() {
    when(processor.toString()).thenReturn(LOCATION);
    when(processor.getProcessingType()).thenReturn(CPU_LITE);
    when(event.getCorrelationId()).thenReturn(CORRELATION_ID);
    streamInterceptor = spy(new TestStreamProcessingStrategyExecutionProfiler(processor, processor.getProcessingType()));
  }

  @Test
  public void logBeforeDispatchingToProcessor() {
    verifyLog(streamInterceptor, event, e -> streamInterceptor.profileBeforeDispatchingToProcessor(event),
              LOG_BEFORE_DISPATCHING_TO_PROCESSOR_TEMPLATE,
              event.getCorrelationId(), CPU_LITE.toString(),
              UNKNOWN_LOCATION_TAG);
  }

  @Test
  public void logBeforeComponentProcessing() {
    verifyLog(streamInterceptor, event, e -> streamInterceptor.profileBeforeComponentProcessing(event),
              LOG_BEFORE_COMPONENT_PROCESSING_TEMPLATE,
              event.getCorrelationId(),
              CPU_LITE.toString(),
              UNKNOWN_LOCATION_TAG);
  }

  @Test
  public void logAfterDispatchingToFlow() {
    verifyLog(streamInterceptor, event, e -> streamInterceptor.profileAfterDispatchingToFlow(event),
              LOG_AFTER_DISPATCHING_TO_FLOW,
              event.getCorrelationId(),
              UNKNOWN_LOCATION_TAG);
  }

  private void verifyLog(LoggerProcessingStrategyExecutionProfiler streamInterceptor, CoreEvent event,
                         Consumer<CoreEvent> traceConsumer,
                         String logTemplate,
                         String correlationId,
                         String location) {
    traceConsumer.accept(event);
    verify(streamInterceptor).log(eq(logTemplate), eq(correlationId), eq(location), anyString(), anyString());
  }

  private void verifyLog(LoggerProcessingStrategyExecutionProfiler streamInterceptor, CoreEvent event,
                         Consumer<CoreEvent> traceConsumer,
                         String logTemplate,
                         String correlationId,
                         String processingType,
                         String location) {
    traceConsumer.accept(event);
    verify(streamInterceptor).log(eq(logTemplate), eq(correlationId), eq(processingType), eq(location), anyString());
  }

  /**
   * Test class to verify that a logging action is performed.
   */
  private class TestStreamProcessingStrategyExecutionProfiler extends LoggerProcessingStrategyExecutionProfiler {

    public TestStreamProcessingStrategyExecutionProfiler(ReactiveProcessor processor,
                                                         ProcessingType processingType) {
      super(null, processingType);
    }

    @Override
    protected void log(String template, String... parameters) {
      super.log(template, parameters);
    }
  }
}
