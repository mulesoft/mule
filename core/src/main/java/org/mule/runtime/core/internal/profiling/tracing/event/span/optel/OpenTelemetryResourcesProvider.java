/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.optel;

import org.mule.runtime.core.internal.profiling.tracing.event.span.optel.config.impl.SystemPropertyOpentelemetryExporterConfiguration;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;

/**
 * Utility class that provides resources for OpTel Tracing
 *
 * @since 4.5.0
 */
public class OpenTelemetryResourcesProvider {


  private OpenTelemetryResourcesProvider() {}

  private static final String MULE_INSTRUMENTATION_NAME = "mule-tracer";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  private static final Tracer OPEN_TELEMETRY_TRACER = createTracer();

  private static Tracer createTracer() {
    SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor
            .builder(OtlpGrpcSpanExporter.builder()
                .setEndpoint(new SystemPropertyOpentelemetryExporterConfiguration().getEndpoint()).build())
            .build())
        .build();

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal();

    return openTelemetry.getTracer(MULE_INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
  }

  /**
   * @return an {@link Tracer}
   */
  public static Tracer getOpentelemetryTracer() {
    return OPEN_TELEMETRY_TRACER;
  }

}
