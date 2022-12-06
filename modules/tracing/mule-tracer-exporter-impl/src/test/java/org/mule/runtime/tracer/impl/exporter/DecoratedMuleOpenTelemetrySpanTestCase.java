/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.exporter;

import static org.mule.runtime.tracer.impl.exporter.OpenTelemetryResources.SERVICE_NAME_KEY;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;

import static java.lang.System.nanoTime;

import static io.opentelemetry.sdk.resources.Resource.getDefault;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.profiling.tracing.SpanDuration;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(PROFILING)
public class DecoratedMuleOpenTelemetrySpanTestCase extends AbstractMuleTestCase {

  @Test
  public void ifAttributeIsPresentSpanKindMustBeUpdated() {
    String spanKind = "CLIENT";

    InternalSpan internalSpan = mock(InternalSpan.class);
    SpanDuration spanDuration = mock(SpanDuration.class);
    when(spanDuration.getStart()).thenReturn(nanoTime());
    when(internalSpan.getDuration()).thenReturn(spanDuration);
    Map<String, String> spanAttributes = new HashMap<>();
    spanAttributes.put(DecoratedMuleOpenTelemetrySpan.SPAN_KIND, spanKind);
    when(internalSpan.getAttributes()).thenReturn(spanAttributes);

    InitialSpanInfo initialSpanInfo = mock(InitialSpanInfo.class);

    OtlpGrpcSpanExporter otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder().build();
    SdkTracerProviderBuilder sdkTracerProviderBuilder =
        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(otlpGrpcSpanExporter));
    Resource resource = getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, "testServiceName")));
    SdkTracerProvider sdkTracerProvider = sdkTracerProviderBuilder.setResource(resource).build();
    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .build();
    Span openTelemetrySpan = openTelemetry.getTracer("testInstrumentationName").spanBuilder("testSpan").startSpan();

    DecoratedMuleOpenTelemetrySpan decoratedMuleOpenTelemetrySpan = new DecoratedMuleOpenTelemetrySpan(openTelemetrySpan);
    decoratedMuleOpenTelemetrySpan.end(internalSpan, initialSpanInfo, "testArtifactId", "testArtifactType");

    assertThat(((ReadableSpan) openTelemetrySpan).getKind(), equalTo(SpanKind.valueOf(spanKind)));
  }

  @Test
  public void ifAttributeIsNotPresentSpanKindMustNotBeUpdated() {
    String spanKind = "INTERNAL";

    InternalSpan internalSpan = mock(InternalSpan.class);
    SpanDuration spanDuration = mock(SpanDuration.class);
    when(spanDuration.getStart()).thenReturn(nanoTime());
    when(internalSpan.getDuration()).thenReturn(spanDuration);

    InitialSpanInfo initialSpanInfo = mock(InitialSpanInfo.class);

    OtlpGrpcSpanExporter otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder().build();
    SdkTracerProviderBuilder sdkTracerProviderBuilder =
        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(otlpGrpcSpanExporter));
    Resource resource = getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, "testServiceName")));
    SdkTracerProvider sdkTracerProvider = sdkTracerProviderBuilder.setResource(resource).build();
    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .build();
    Span openTelemetrySpan = openTelemetry.getTracer("testInstrumentationName").spanBuilder("testSpan").startSpan();

    DecoratedMuleOpenTelemetrySpan decoratedMuleOpenTelemetrySpan = new DecoratedMuleOpenTelemetrySpan(openTelemetrySpan);
    decoratedMuleOpenTelemetrySpan.end(internalSpan, initialSpanInfo, "testArtifactId", "testArtifactType");

    assertThat(((ReadableSpan) openTelemetrySpan).getKind(), equalTo(SpanKind.valueOf(spanKind)));
  }

  @Test
  public void ifAttributeIsPresentSpanStatusMustBeUpdated() {
    String spanStatus = "OK";

    InternalSpan internalSpan = mock(InternalSpan.class);
    SpanDuration spanDuration = mock(SpanDuration.class);
    when(spanDuration.getStart()).thenReturn(nanoTime());
    when(internalSpan.getDuration()).thenReturn(spanDuration);
    Map<String, String> spanAttributes = new HashMap<>();
    spanAttributes.put(DecoratedMuleOpenTelemetrySpan.STATUS, spanStatus);
    when(internalSpan.getAttributes()).thenReturn(spanAttributes);

    InitialSpanInfo initialSpanInfo = mock(InitialSpanInfo.class);

    OtlpGrpcSpanExporter otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder().build();
    SdkTracerProviderBuilder sdkTracerProviderBuilder =
        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(otlpGrpcSpanExporter));
    Resource resource = getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, "testServiceName")));
    SdkTracerProvider sdkTracerProvider = sdkTracerProviderBuilder.setResource(resource).build();
    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .build();
    Span openTelemetrySpan = openTelemetry.getTracer("testInstrumentationName").spanBuilder("testSpan").startSpan();

    DecoratedMuleOpenTelemetrySpan decoratedMuleOpenTelemetrySpan = new DecoratedMuleOpenTelemetrySpan(openTelemetrySpan);
    decoratedMuleOpenTelemetrySpan.end(internalSpan, initialSpanInfo, "testArtifactId", "testArtifactType");

    assertThat(openTelemetrySpan.toString(), containsString("status=ImmutableStatusData{statusCode=OK, description=}"));
  }

  @Test
  public void ifAttributeIsNotPresentSpanStatusMustNotBeUpdated() {
    InternalSpan internalSpan = mock(InternalSpan.class);
    SpanDuration spanDuration = mock(SpanDuration.class);
    when(spanDuration.getStart()).thenReturn(nanoTime());
    when(internalSpan.getDuration()).thenReturn(spanDuration);

    InitialSpanInfo initialSpanInfo = mock(InitialSpanInfo.class);

    OtlpGrpcSpanExporter otlpGrpcSpanExporter = OtlpGrpcSpanExporter.builder().build();
    SdkTracerProviderBuilder sdkTracerProviderBuilder =
        SdkTracerProvider.builder().addSpanProcessor(SimpleSpanProcessor.create(otlpGrpcSpanExporter));
    Resource resource = getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, "testServiceName")));
    SdkTracerProvider sdkTracerProvider = sdkTracerProviderBuilder.setResource(resource).build();
    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .build();
    Span openTelemetrySpan = openTelemetry.getTracer("testInstrumentationName").spanBuilder("testSpan").startSpan();

    DecoratedMuleOpenTelemetrySpan decoratedMuleOpenTelemetrySpan = new DecoratedMuleOpenTelemetrySpan(openTelemetrySpan);
    decoratedMuleOpenTelemetrySpan.end(internalSpan, initialSpanInfo, "testArtifactId", "testArtifactType");

    assertThat(openTelemetrySpan.toString(), containsString("status=ImmutableStatusData{statusCode=UNSET, description=}"));
  }
}
