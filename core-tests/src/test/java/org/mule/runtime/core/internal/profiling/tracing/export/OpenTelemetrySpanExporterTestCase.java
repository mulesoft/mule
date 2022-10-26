/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.export;

import static org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel.OpenTelemetryResourcesProvider.getOpenTelemetryTracer;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.util.Collections.emptySet;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.core.internal.execution.tracing.DistributedTraceContextAware;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.trace.DistributedTraceContext;

import com.google.common.collect.ImmutableMap;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class OpenTelemetrySpanExporterTestCase {

  public static final String TRACEPARENT_KEY = "traceparent";
  public static final String PARENT_TRACE_ID = "80e1afed08e019fc1110464cfa66635c";
  public static final String PARENT_SPAN_ID = "7a085853722dc6d2";
  public static final String TEST_SERVICE_NAME = "test-service";
  public static final String TEST_SPAN_NAME = "testSpanName";

  @Test
  public void createOpentelemetrySpanExporterWithRemoteW3CContextParent() {
    DistributedTraceContextAware eventContext =
        mock(DistributedTraceContextAware.class, withSettings().extraInterfaces(EventContext.class));
    DistributedTraceContext distributedTraceContext = mock(DistributedTraceContext.class);

    when(eventContext.getDistributedTraceContext()).thenReturn(distributedTraceContext);
    when(distributedTraceContext.tracingFieldsAsMap())
        .thenReturn(ImmutableMap.of(TRACEPARENT_KEY, "00-" + PARENT_TRACE_ID + "-" + PARENT_SPAN_ID + "-01"));

    // mock for the internal mule span
    InternalSpan internalMuleSpan = mock(InternalSpan.class);
    SpanDuration spanDuration = mock(SpanDuration.class);

    when(internalMuleSpan.getDuration()).thenReturn(spanDuration);
    when(internalMuleSpan.getName()).thenReturn(TEST_SPAN_NAME);
    OpenTelemetrySpanExporter openTelemetrySpanExporter = new OpenTelemetrySpanExporter(
                                                                                        getOpenTelemetryTracer(new TestSpanConfiguration(),
                                                                                                               TEST_SERVICE_NAME),
                                                                                        (EventContext) eventContext,
                                                                                        true,
                                                                                        emptySet(),
                                                                                        internalMuleSpan);
    assertThat(openTelemetrySpanExporter.getOpenTelemetrySpan().getSpanContext().getTraceId(), equalTo(PARENT_TRACE_ID));
    assertThat(((ReadableSpan) openTelemetrySpanExporter.getOpenTelemetrySpan()).getParentSpanContext().getTraceId(), equalTo(
                                                                                                                              PARENT_TRACE_ID));
    assertThat(((ReadableSpan) openTelemetrySpanExporter.getOpenTelemetrySpan()).getParentSpanContext().getSpanId(), equalTo(
                                                                                                                             PARENT_SPAN_ID));
  }

  /**
   * A {@link SpanExporterConfiguration} for testing purposes.
   */
  private static class TestSpanConfiguration implements SpanExporterConfiguration {

    @Override
    public String getValue(String key) {
      return null;
    }
  }
}
