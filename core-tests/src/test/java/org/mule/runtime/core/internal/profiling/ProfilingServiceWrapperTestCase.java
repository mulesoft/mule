/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling;


import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_PROFILING_SERVICE;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.CoreEventTracer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_PROFILING_SERVICE)
public class ProfilingServiceWrapperTestCase {

  public static final CoreEventTracer MOCK_CORE_EVENT_TRACER = mock(CoreEventTracer.class);
  public static final FeatureFlaggingService MOCK_FEATURE_FLAGGING_SERVICE = mock(FeatureFlaggingService.class);

  @Test
  public void verifyCoreEventTracerEnabled() {
    SpanExporterConfiguration spanExporterConfiguration = mock(SpanExporterConfiguration.class);
    when(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false")).thenReturn(TRUE.toString());
    ProfilingServiceWrapper profilingServiceWrapper = new TestProfilingServiceWrapper(spanExporterConfiguration);

    EventTracer<CoreEvent> coreEventTracer = profilingServiceWrapper.getCoreEventTracer();
    assertThat(coreEventTracer, instanceOf(SelectableCoreEventTracer.class));
    assertThat(((SelectableCoreEventTracer) coreEventTracer).getCurrentEventTracer(), equalTo(MOCK_CORE_EVENT_TRACER));

    // We also verify that the core event tracer is the same when retrieved again.
    assertThat(profilingServiceWrapper.getCoreEventTracer(), equalTo(coreEventTracer));
  }

  @Test
  public void verifyCoreEventTracerDisabled() {
    SpanExporterConfiguration spanExporterConfiguration = mock(SpanExporterConfiguration.class);
    when(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false")).thenReturn(FALSE.toString());
    ProfilingServiceWrapper profilingServiceWrapper = new TestProfilingServiceWrapper(spanExporterConfiguration);
    EventTracer<CoreEvent> coreEventTracer = profilingServiceWrapper.getCoreEventTracer();
    assertThat(coreEventTracer, instanceOf(SelectableCoreEventTracer.class));
    assertThat(((SelectableCoreEventTracer) coreEventTracer).getCurrentEventTracer(), instanceOf(NoopCoreEventTracer.class));

    // We also verify that the core event tracer is the same when retrieved again.
    assertThat(profilingServiceWrapper.getCoreEventTracer(), equalTo(coreEventTracer));
  }

  private static class TestProfilingServiceWrapper extends ProfilingServiceWrapper {

    public TestProfilingServiceWrapper(SpanExporterConfiguration spanExporterConfiguration) {
      this.coreEventTracer = MOCK_CORE_EVENT_TRACER;
      this.featureFlaggingService = MOCK_FEATURE_FLAGGING_SERVICE;
      this.spanExporterConfiguration = spanExporterConfiguration;
      this.muleContext = mock(MuleContext.class);
      when(muleContext.getInjector()).thenReturn(mock(Injector.class));
    }
  }
}
