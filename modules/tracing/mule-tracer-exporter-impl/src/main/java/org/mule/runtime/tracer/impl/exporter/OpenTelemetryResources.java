/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter;

import static java.lang.Boolean.parseBoolean;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.context.propagation.ContextPropagators.create;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SIZE;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporterBuilder;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.tracer.exporter.api.config.SpanExporterConfiguration;
import org.mule.runtime.tracer.impl.exporter.capturer.CapturingSpanExporterWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.mule.runtime.tracer.impl.exporter.config.type.OpenTelemetryExporterType;

public class OpenTelemetryResources {

  private static final ContextPropagators PROPAGATOR = create(W3CTraceContextPropagator.getInstance());

  // This is only defined in the semconv artifact which is in alpha state and is only needed for this.
  // In order not to add another dependency we add it here.
  // For the moment it is defined in the spec here:
  // TODO: W-11610439: tracking: verify if the semconv dependency (alpha) should be added
  // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md#semantic-attributes-with-dedicated-environment-variable
  public static final AttributeKey<String> SERVICE_NAME_KEY = stringKey("service.name");

  private static final String MULE_INSTRUMENTATION_NAME = "mule-tracer";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

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

  public static Tracer getTracer(SpanExporterConfiguration spanExporterConfiguration, String serviceName) {
    SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder();

    Resource resource = Resource.getDefault()
        .merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, serviceName)));

    if (parseBoolean(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, "false"))) {
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

  private static SpanProcessor resolveExporterProcessor(
                                                        SpanExporterConfiguration spanExporterConfiguration) {
    if (Integer.parseInt(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SIZE)) == 0) {
      return SimpleSpanProcessor.create(createExporter(spanExporterConfiguration));
    }

    return BatchSpanProcessor.builder(createExporter(spanExporterConfiguration))
        .build();
  }

  private static SpanExporter createExporter(SpanExporterConfiguration spanExporterConfiguration) {
    if (spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_TYPE).equals(OpenTelemetryExporterType.GRPC.toString())) {
      return createGrpcExporter(spanExporterConfiguration);
    }
    String endpoint = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT);
    if (!isEmpty(endpoint)) {
      return OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build();
    } else {
      return OtlpGrpcSpanExporter.builder().build();
    }
  }

  private static SpanExporter createGrpcExporter(SpanExporterConfiguration spanExporterConfiguration) {
    String endpoint = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT);
    if (!isEmpty(endpoint)) {
      OtlpGrpcSpanExporterBuilder builder = OtlpGrpcSpanExporter.builder().setEndpoint(endpoint);
      if (spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_TLS_ENABLED, "false").equals(Boolean.TRUE.toString())) {
        configureTls(builder, spanExporterConfiguration);
      }
      return builder.build();
    }

    return OtlpGrpcSpanExporter.builder().build();
  }

  private static void configureTls(OtlpGrpcSpanExporterBuilder builder, SpanExporterConfiguration spanExporterConfiguration) {
    String keyFilePath = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_KEY_FILE_LOCATION);
    String certFilePath = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_CERT_FILE_LOCATION);
    String caFilePath = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_CA_FILE_LOCATION);
    try {
      byte[] keyFileBytes = Files.readAllBytes(Paths.get(keyFilePath));
      byte[] certFileBytes = Files.readAllBytes(Paths.get(certFilePath));
      byte[] caFileBytes = Files.readAllBytes(Paths.get(caFilePath));

      builder.setClientTls(keyFileBytes, certFileBytes);
      builder.setTrustedCertificates(caFileBytes);
    } catch (IOException e) {

    }

  }

}
