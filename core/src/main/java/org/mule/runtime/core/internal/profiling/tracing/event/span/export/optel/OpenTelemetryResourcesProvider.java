/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.export.optel;

import static org.mule.runtime.core.api.util.StringUtils.isEmpty;
import static org.mule.runtime.core.internal.profiling.tracing.export.OpentelemetrySpanExporter.OPENTELEMETRY_SPAN_VISITOR;

import static java.lang.Boolean.parseBoolean;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.context.propagation.ContextPropagators.create;

import org.mule.runtime.core.internal.profiling.tracing.event.span.ExecutionSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.ExportOnEndSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpanVisitor;
import org.mule.runtime.core.internal.profiling.tracing.event.span.export.CapturingSpanExporterWrapper;
import org.mule.runtime.core.internal.profiling.tracing.export.SpanExporterConfiguration;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * Utility class that provides resources for OpTel Tracing
 *
 * @since 4.5.0
 */
public class OpenTelemetryResourcesProvider {


  private static final OpenTelemetryContextVisitor OPTEL_CONTEXT_VISITOR = new OpenTelemetryContextVisitor();

  // Insert the context as Header
  private final static TextMapSetter<Map<String, String>> SETTER =
      Map::put;

  private final static CapturingSpanExporterWrapper capturingSpanExporterWrapper =
      new CapturingSpanExporterWrapper(new SpanExporter() {

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

  private static final String OPENTELEMETRY_EXPORT_ENABLED_SYSPROP = "mule.openetelemetry.export.enabled";
  private static final String MULE_OPENTELEMETRY_ENDPOINT_SYSPROP = "mule.opentelemetry.endpoint";
  // This is only defined in the semconv artifact which is in alpha state and is only needed for this.
  // In order not to add another dependency we add it here.
  // For the moment it is defined in the spec here:
  // TODO: W-11610439: tracking: verify if the semconv dependency (alpha) should be added
  // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md#semantic-attributes-with-dedicated-environment-variable
  public static final AttributeKey<String> SERVICE_NAME_KEY = stringKey("service.name");
  private static ContextPropagators propagator;

  private OpenTelemetryResourcesProvider() {}

  private static final String MULE_INSTRUMENTATION_NAME = "mule-tracer";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  public static Tracer getOpenTelemetryTracer(SpanExporterConfiguration spanExporterConfiguration, String serviceName) {
    SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder();

    Resource resource = Resource.getDefault()
        .merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, serviceName)));

    if (parseBoolean(spanExporterConfiguration.getValue(OPENTELEMETRY_EXPORT_ENABLED_SYSPROP))) {
      sdkTracerProviderBuilder = sdkTracerProviderBuilder.addSpanProcessor(resolveExporterProcessor(spanExporterConfiguration));
    } else {
      sdkTracerProviderBuilder =
          sdkTracerProviderBuilder.addSpanProcessor(resolveDummyExporterWithCapturer(spanExporterConfiguration));
    }

    SdkTracerProvider sdkTracerProvider = sdkTracerProviderBuilder.setResource(resource).build();

    OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(getPropagator())
        .build();

    return openTelemetry.getTracer(MULE_INSTRUMENTATION_NAME, INSTRUMENTATION_VERSION);
  }

  private static SpanProcessor resolveDummyExporterWithCapturer(SpanExporterConfiguration spanExporterConfiguration) {
    return SimpleSpanProcessor.create(capturingSpanExporterWrapper);
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

  /**
   * Gets the default propagator.
   *
   * @return the propagator defined by default.
   */
  public static ContextPropagators getPropagator() {
    if (propagator == null) {
      propagator = create(W3CTraceContextPropagator.getInstance());
    }
    return propagator;
  }

  /**
   * @param span the span for the distributed trace context to retrieve
   * @return a map containing the span context to propagate.
   */
  public static Map<String, String> getDistributedTraceContextMap(InternalSpan span) {
    Map<String, String> contextMap = new HashMap<>();
    Optional<Context> optionalContext = span.visit(OPTEL_CONTEXT_VISITOR);
    optionalContext.ifPresent(context -> getPropagator().getTextMapPropagator().inject(context, contextMap, SETTER));
    return contextMap;
  }

  private static class OpenTelemetryContextVisitor implements InternalSpanVisitor<Optional<Context>> {

    @Override
    public Optional<Context> accept(ExportOnEndSpan exportOnEndSpan) {
      return Optional.of(Context.current().with(exportOnEndSpan.getSpanExporter().visit(OPENTELEMETRY_SPAN_VISITOR)));
    }

    @Override
    public Optional<Context> accept(ExecutionSpan executionSpan) {
      return Optional.empty();
    }

    @Override
    public Optional<Context> accept(InternalSpan.SpanInternalWrapper spanInternalWrapper) {
      return Optional.empty();
    }
  }
}
