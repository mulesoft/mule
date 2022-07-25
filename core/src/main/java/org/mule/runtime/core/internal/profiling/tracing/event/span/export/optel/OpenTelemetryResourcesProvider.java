/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import static java.lang.Boolean.getBoolean;

import org.mule.runtime.core.internal.profiling.tracing.event.span.export.CapturingSpanExporterWrapper;
import org.mule.runtime.core.internal.profiling.tracing.export.SpanExporterConfiguration;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import java.util.Collection;

/**
 * Utility class that provides resources for OpTel Tracing
 *
 * @since 4.5.0
 */
public class OpenTelemetryResourcesProvider {


  private static CapturingSpanExporterWrapper capturingSpanExporterWrapper;
  private static final String OPENTELEMETRY_EXPORT_ENABLED_SYSPROP = "mule.openetelemetry.export.enabled";
  private static final String MULE_OPENTELEMETRY_ENDPOINT_SYSPROP = "mule.opentelemetry.endpoint";

  private OpenTelemetryResourcesProvider() {}

  private static final String MULE_INSTRUMENTATION_NAME = "mule-tracer";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  public static Tracer getOpenTelemetryTracer(SpanExporterConfiguration spanExporterConfiguration) {
    SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder();

    if (getBoolean(spanExporterConfiguration.getValue(OPENTELEMETRY_EXPORT_ENABLED_SYSPROP))) {
      sdkTracerProviderBuilder = sdkTracerProviderBuilder.addSpanProcessor(resolveExporterProcessor(spanExporterConfiguration));
    } else {
      sdkTracerProviderBuilder =
          sdkTracerProviderBuilder.addSpanProcessor(resolveDummyExporterWithCapturer(spanExporterConfiguration));
    }

    SdkTracerProvider sdkTracerProvider = sdkTracerProviderBuilder.build();

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal();

    return openTelemetry.getTracer(MULE_INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
  }

  private static SpanProcessor resolveDummyExporterWithCapturer(SpanExporterConfiguration spanExporterConfiguration) {

    CapturingSpanExporterWrapper spanExporter = new CapturingSpanExporterWrapper(new SpanExporter() {

      @Override
      public CompletableResultCode export(Collection<SpanData> collection) {
        return new CompletableResultCode().succeed();
      }

      @Override
      public CompletableResultCode flush() {
        return new CompletableResultCode().succeed();
      }

      @Override
      public CompletableResultCode shutdown() {
        return new CompletableResultCode().succeed();
      }
    });

    capturingSpanExporterWrapper = spanExporter;

    return SimpleSpanProcessor.create(spanExporter);
  }

  public static ExportedSpanCapturer getNewExportedSpanCapturer() {
    return capturingSpanExporterWrapper.getSpanCapturer();
  }

  private static SpanProcessor resolveExporterProcessor(SpanExporterConfiguration spanExporterConfiguration) {
    return BatchSpanProcessor.builder(createExporter(spanExporterConfiguration.getValue(MULE_OPENTELEMETRY_ENDPOINT_SYSPROP)))
        .build();
  }

  private static SpanExporter createExporter(String endpoint) {
    if (!isEmpty(endpoint)) {
      return OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build();
    } else {
      return OtlpGrpcSpanExporter.builder().build();
    }
  }
}
