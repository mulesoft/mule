/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracing.level.impl.config;

import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.OVERVIEW;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CONFIGURATION;

import static java.lang.Boolean.TRUE;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.runtime.tracing.level.api.config.TracingLevelConfiguration;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(TRACING_CONFIGURATION)
public class AutoConfigurableTracingLevelConfigurationTestCase {

  private static final TracingLevel DEFAULT_LEVEL = MONITORING;

  @Test
  public void returnValueFromDelegate() {
    MuleContext muleContext = mock(MuleContext.class);
    TracingLevelConfiguration delegate =
        mock(TracingLevelConfiguration.class);
    when(delegate.getTracingLevel()).thenReturn(OVERVIEW);
    AutoConfigurableTracingLevelConfiguration tracingLevelConfiguration =
        new AutoConfigurableTracingLevelConfiguration(muleContext, delegate);
    tracingLevelConfiguration.setSpanExporterConfiguration(mock(SpanExporterConfiguration.class));
    assertThat(tracingLevelConfiguration.getTracingLevel(), is(OVERVIEW));
  }

  @Test
  public void whenDelegateReturnsNullDefaultLevelIsReturned() {
    MuleContext muleContext = mock(MuleContext.class);
    TracingLevelConfiguration delegate =
        mock(TracingLevelConfiguration.class);
    when(delegate.getTracingLevel()).thenReturn(null);
    AutoConfigurableTracingLevelConfiguration tracingLevelConfiguration =
        new AutoConfigurableTracingLevelConfiguration(muleContext, delegate);
    tracingLevelConfiguration.setSpanExporterConfiguration(mock(SpanExporterConfiguration.class));
    assertThat(tracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

  @Test
  public void whenDelegateThrowsExceptionDefaultLevelIsReturned() {
    SpanExporterConfiguration spanExporterConfiguration = mock(SpanExporterConfiguration.class);
    when(spanExporterConfiguration.getStringValue(any(), any())).thenReturn(TRUE.toString());
    MuleContext muleContext = mock(MuleContext.class);
    TracingLevelConfiguration delegate =
        mock(TracingLevelConfiguration.class);
    when(delegate.getTracingLevel()).thenThrow(new MuleRuntimeException(new IllegalArgumentException()));
    AutoConfigurableTracingLevelConfiguration tracingLevelConfiguration =
        new AutoConfigurableTracingLevelConfiguration(muleContext, delegate);
    tracingLevelConfiguration.setSpanExporterConfiguration(spanExporterConfiguration);
    assertThat(tracingLevelConfiguration.getTracingLevel(), is(DEFAULT_LEVEL));
  }

}
