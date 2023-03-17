/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.OPEN_TELEMETRY_EXPORTER;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_EXPORTER)
public class OpenTelemetrySpanExporterFactoryTestCase {

  SpanProcessor mockedSpanProcessor = mock(SpanProcessor.class);

  @Test
  public void disposeMustShutdownSpanProcessor() {
    OpenTelemetrySpanExporterFactory spanExporterFactory = new OpenTelemetrySpanExporterFactory() {

      @Override
      protected SpanProcessor resolveOpenTelemetrySpanProcessor() {
        return mockedSpanProcessor;
      }
    };
    spanExporterFactory.dispose();
    verify(mockedSpanProcessor, times(1)).shutdown();
  }

}
