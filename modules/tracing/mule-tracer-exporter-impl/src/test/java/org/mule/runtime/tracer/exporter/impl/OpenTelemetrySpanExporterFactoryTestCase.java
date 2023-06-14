/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.exporter.impl;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.OPEN_TELEMETRY_EXPORTER;

import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.MuleContext;

import io.opentelemetry.sdk.trace.SpanProcessor;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(OPEN_TELEMETRY_EXPORTER)
public class OpenTelemetrySpanExporterFactoryTestCase {

  @Test
  public void disposeMustShutdownSpanProcessor() throws Exception {
    MuleContext muleContext = mock(MuleContext.class);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    FeatureFlaggingService featureFlaggingService = mock(FeatureFlaggingService.class);
    SpanProcessor mockedSpanProcessor = mock(SpanProcessor.class);

    when(muleConfiguration.getId()).thenReturn(randomNumeric(3));
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    when(muleContext.getArtifactType()).thenReturn(APP);

    OpenTelemetrySpanExporterFactory spanExporterFactory = new OpenTelemetrySpanExporterFactory() {

      @Override
      protected SpanProcessor resolveOpenTelemetrySpanProcessor() {
        return mockedSpanProcessor;
      }
    };

    spanExporterFactory.setMuleContext(muleContext);
    spanExporterFactory.setFeatureFlaggingService(featureFlaggingService);
    spanExporterFactory.initialise();
    spanExporterFactory.dispose();
    verify(mockedSpanProcessor, times(1)).shutdown();
  }

}
