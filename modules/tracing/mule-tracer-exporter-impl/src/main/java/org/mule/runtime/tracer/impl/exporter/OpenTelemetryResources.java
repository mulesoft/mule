/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.context.propagation.ContextPropagators.create;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.System.getProperty;

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporterBuilder;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.exporter.capturer.CapturingSpanExporterWrapper;

import java.util.Collection;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class OpenTelemetryResources {

  private static final ContextPropagators PROPAGATOR = create(W3CTraceContextPropagator.getInstance());

  private static final String OPENTELEMETRY_EXPORT_ENABLED_SYSPROP = "mule.openetelemetry.export.enabled";
  private static final String MULE_OPENTELEMETRY_ENDPOINT_SYSPROP = "mule.opentelemetry.endpoint";
  private static final String MULE_OPENTELEMETRY_PROTOCOL_SYSPROP = "mule.opentelemetry.export.protocol";
  private static final String MULE_OPENTELEMETRY_EXPORT_BATCH_SIZE_SYSPROP = "mule.opentelemetry.export.batch.size";
  private static final String MULE_OPENTELEMETRY_EXPORT_BATCH_QUEUE_SIZE_SYSPROP = "mule.opentelemetry.export.queue.size";

  private static final String HTTP_PROTOCOL_NAME = "HTTP";

  // This is only defined in the semconv artifact which is in alpha state and is only needed for this.
  // In order not to add another dependency we add it here.
  // For the moment it is defined in the spec here:
  // TODO: W-11610439: tracking: verify if the semconv dependency (alpha) should be added
  // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md#semantic-attributes-with-dedicated-environment-variable
  public static final AttributeKey<String> SERVICE_NAME_KEY = stringKey("service.name");

  private static final String MULE_INSTRUMENTATION_NAME = "mule-tracer";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  private OpenTelemetryResources() {}

  private static final CapturingSpanExporterWrapper capturingSpanExporterWrapper =
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

  public static Tracer getTracer(SpanExporterConfiguration spanExporterConfiguration, String serviceName) {
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

  public static ExportedSpanSniffer getNewExportedSpanCapturer() {
    return capturingSpanExporterWrapper.getSpanCapturer();
  }

  private static SpanProcessor resolveDummyExporterWithCapturer(SpanExporterConfiguration spanExporterConfiguration) {
    return SimpleSpanProcessor.create(capturingSpanExporterWrapper);
  }

  public static ContextPropagators getPropagator() {
    return PROPAGATOR;
  }

  private static SpanProcessor resolveExporterProcessor(SpanExporterConfiguration spanExporterConfiguration) {
    createSpanExporter(spanExporterConfiguration);
    return BatchSpanProcessor.builder(createSpanExporter(spanExporterConfiguration))
        .setMaxQueueSize(parseInt(getProperty(MULE_OPENTELEMETRY_EXPORT_BATCH_QUEUE_SIZE_SYSPROP, "2048")))
        .setMaxExportBatchSize(parseInt(getProperty(MULE_OPENTELEMETRY_EXPORT_BATCH_SIZE_SYSPROP, "512")))
        .build();
  }

  private static SpanExporter createSpanExporter(SpanExporterConfiguration spanExporterConfiguration) {
    String endpoint = spanExporterConfiguration.getValue(MULE_OPENTELEMETRY_ENDPOINT_SYSPROP);
    if (HTTP_PROTOCOL_NAME.equalsIgnoreCase(spanExporterConfiguration.getValue(MULE_OPENTELEMETRY_PROTOCOL_SYSPROP))) {
      return createHttpExporter(endpoint);
    } else {
      return createGrpcExporter(endpoint);
    }
  }

  private static SpanExporter createHttpExporter(String endpoint) {
    OtlpHttpSpanExporterBuilder exporterBuilder = OtlpHttpSpanExporter.builder();
    if (!isEmpty(endpoint)) {
      exporterBuilder.setEndpoint(endpoint);
    }
    return exporterBuilder.build();
  }

  private static SpanExporter createGrpcExporter(String endpoint) {
    OtlpGrpcSpanExporterBuilder exporterBuilder = OtlpGrpcSpanExporter.builder();
    if (!isEmpty(endpoint)) {
      exporterBuilder.setEndpoint(endpoint);
    }
    return exporterBuilder.build();
  }

}
