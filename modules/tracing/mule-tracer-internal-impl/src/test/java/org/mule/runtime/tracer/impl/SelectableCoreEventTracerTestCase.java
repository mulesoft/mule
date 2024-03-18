/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl;

import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import static org.mockito.Mockito.when;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class SelectableCoreEventTracerTestCase {

  @Test
  public void testCoreEventTracerEnabled() throws InitialisationException {
    testExpectedSelectedCoreEventTracer(TRUE.toString(), CoreEventTracer.class);
  }

  @Test
  public void testCoreEventTracerDisabled() throws InitialisationException {
    testExpectedSelectedCoreEventTracer(FALSE.toString(), NoopCoreEventTracer.class);
  }

  @Test
  public void defaultSelectedCoreEventTracer() {
    SelectableCoreEventTracer selectableCoreEventTracerTestCase = new SelectableCoreEventTracer();
    assertThat(selectableCoreEventTracerTestCase.getSelectedCoreEventTracer(), instanceOf(NoopCoreEventTracer.class));
  }


  private static void testExpectedSelectedCoreEventTracer(String expectedConfigurationValue,
                                                          Class<? extends EventTracer> coreEventTracerClass)
      throws InitialisationException {
    SelectableCoreEventTracer selectableCoreEventTracerTestCase = new SelectableCoreEventTracer();
    SpanExporterConfiguration spanExporterConfiguration = mock(SpanExporterConfiguration.class);
    selectableCoreEventTracerTestCase.setSpanExporterConfiguration(spanExporterConfiguration);
    selectableCoreEventTracerTestCase.setFeatureFlaggingService(mock(FeatureFlaggingService.class));
    selectableCoreEventTracerTestCase.setEventSpanFactory(mock(EventSpanFactory.class));
    when(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false"))
        .thenReturn(expectedConfigurationValue);
    selectableCoreEventTracerTestCase.initialise();
    assertThat(selectableCoreEventTracerTestCase.getSelectedCoreEventTracer(), instanceOf(coreEventTracerClass));
  }
}
