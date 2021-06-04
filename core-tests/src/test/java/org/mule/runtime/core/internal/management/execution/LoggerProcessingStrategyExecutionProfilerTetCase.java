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
import org.mule.runtime.core.internal.management.execution.LoggerProcessingStrategyExecutionProfiler;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.core.internal.management.execution.LoggerProcessingStrategyExecutionProfiler.UNKNOWN_LOCATION_TAG;
import static org.mule.runtime.core.internal.management.execution.LoggerProcessingStrategyExecutionProfiler.LOG_BEFORE_COMPONENT_PROCESSING_TEMPLATE;
import static org.mule.runtime.core.internal.management.execution.LoggerProcessingStrategyExecutionProfiler.LOG_BEFORE_DISPATCHING_TO_PROCESSOR_TEMPLATE;
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
    when(event.getCorrelationId()).thenReturn(CORRELATION_ID);
    streamInterceptor = spy(new TestStreamProcessingStrategyExecutionProfiler(processor));
  }

  @Test
  public void logBeforeDispatchingToProcessor() {
    verifyLog(streamInterceptor, event, e -> streamInterceptor.profileBeforeDispatchingToProcessor(event),
              LOG_BEFORE_DISPATCHING_TO_PROCESSOR_TEMPLATE,
              event.getCorrelationId(), UNKNOWN_LOCATION_TAG);
  }

  @Test
  public void logBeforeComponentProcessing() {
    streamInterceptor.profileBeforeDispatchingToProcessor(event);
    verifyLog(streamInterceptor, event, e -> streamInterceptor.profileBeforeComponentProcessing(event),
              LOG_BEFORE_COMPONENT_PROCESSING_TEMPLATE,
              event.getCorrelationId(), UNKNOWN_LOCATION_TAG);
  }

  @Test
  public void logAfterDispatchingToFlow() {
    streamInterceptor.profileAfterDispatchingToFlow(event);
    verify(streamInterceptor).log(anyString(), anyString(), anyString(), anyString());
  }

  private void verifyLog(LoggerProcessingStrategyExecutionProfiler streamInterceptor, CoreEvent event,
                         Consumer<CoreEvent> traceConsumer,
                         String logTemplate,
                         String... parameters) {
    traceConsumer.accept(event);
    verify(streamInterceptor).log(logTemplate, parameters);
  }

  /**
   * Test class to verify that a logging action is performed.
   */
  private class TestStreamProcessingStrategyExecutionProfiler extends LoggerProcessingStrategyExecutionProfiler {

    public TestStreamProcessingStrategyExecutionProfiler(ReactiveProcessor processor) {
      super(null);
    }

    @Override
    protected void log(String template, String... parameters) {
      super.log(template, parameters);
    }
  }
}
