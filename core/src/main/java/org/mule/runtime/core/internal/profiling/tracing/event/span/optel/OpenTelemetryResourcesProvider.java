/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.optel;

import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import org.mule.runtime.core.internal.profiling.tracing.event.span.optel.config.OpentelemetryExporterConfiguration;
import org.mule.runtime.core.internal.profiling.tracing.event.span.optel.config.impl.SystemPropertyOpentelemetryExporterConfiguration;
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
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import java.util.Collection;

/**
 * Utility class that provides resources for OpTel Tracing
 *
 * @since 4.5.0
 */
public class OpenTelemetryResourcesProvider {


  private static CapturingSpanExporterWrapper capturingSpanExporterWrapper;

  private OpenTelemetryResourcesProvider() {}

  private static final String MULE_INSTRUMENTATION_NAME = "mule-tracer";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  private static final Tracer OPEN_TELEMETRY_TRACER = createTracer();

  private static Tracer createTracer() {

    SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder();

    OpentelemetryExporterConfiguration configuration = new SystemPropertyOpentelemetryExporterConfiguration();

    if (configuration.isEnabled()) {
      sdkTracerProviderBuilder = sdkTracerProviderBuilder.addSpanProcessor(resolveExporterProcessor(configuration));
    }

    SdkTracerProvider sdkTracerProvider = sdkTracerProviderBuilder.build();

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal();

    return openTelemetry.getTracer(MULE_INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
  }

  private static SpanProcessor resolveExporterProcessor(
                                                        OpentelemetryExporterConfiguration configuration) {
    String endpoint = configuration.getEndpoint();

    if (!isEmpty(endpoint)) {
      return BatchSpanProcessor.builder(createExporter()).build();
    }

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

  private static SpanExporter createExporter() {
    return OtlpGrpcSpanExporter.builder().setEndpoint(new SystemPropertyOpentelemetryExporterConfiguration().getEndpoint())
        .build();
  }

  /**
   * @return an {@link Tracer}
   */
  public static Tracer getOpentelemetryTracer() {
    return OPEN_TELEMETRY_TRACER;
  }

  public static ExportedSpanCapturer getNewExportedSpanCapturer() {
    return capturingSpanExporterWrapper.getSpanCapturer();
  }

}
