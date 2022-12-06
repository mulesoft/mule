/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.exporter.optel.resources;

import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_MAX_QUEUE_SIZE;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_MAX_SIZE;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENABLED;
import static org.mule.runtime.tracer.impl.exporter.config.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.runtime.tracer.impl.exporter.config.type.OpenTelemetryExporterTransport.valueOf;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.context.propagation.ContextPropagators.create;
import static io.opentelemetry.sdk.resources.Resource.getDefault;
import static io.opentelemetry.sdk.trace.export.BatchSpanProcessor.builder;
import static io.opentelemetry.sdk.trace.export.SimpleSpanProcessor.create;

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
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * A Retriever open telemetry resources.
 *
 * @since 4.5.0
 */
public class OpenTelemetryResources {

  private OpenTelemetryResources() {}

  private static final ContextPropagators PROPAGATOR = create(W3CTraceContextPropagator.getInstance());

  // This is only defined in the semconv artifact which is in alpha state and is only needed for this.
  // In order not to add another dependency we add it here.
  // For the moment it is defined in the spec here:
  // TODO: W-11610439: tracking: verify if the semconv dependency (alpha) should be added
  // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md#semantic-attributes-with-dedicated-environment-variable
  public static final AttributeKey<String> SERVICE_NAME_KEY = stringKey("service.name");

  private static final String MULE_INSTRUMENTATION_NAME = "mule-tracer";

  private static final String INSTRUMENTATION_VERSION = "1.0.0";

  private static final CapturingSpanExporterWrapper capturingSpanExporterWrapper =
      new CapturingSpanExporterWrapper(new DummySpanExporter());

  public static Tracer getTracer(SpanExporterConfiguration spanExporterConfiguration, String serviceName)
      throws SpanExporterConfiguratorException {
    SdkTracerProviderBuilder sdkTracerProviderBuilder = SdkTracerProvider.builder();

    Resource resource = getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, serviceName)));

    // Verify if the opentelemetry span exporter is enabled.
    if (parseBoolean(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_ENABLED, FALSE.toString()))) {
      sdkTracerProviderBuilder = sdkTracerProviderBuilder.addSpanProcessor(resolveExporterProcessor(spanExporterConfiguration));
    } else {
      sdkTracerProviderBuilder =
          sdkTracerProviderBuilder.addSpanProcessor(resolveDummyExporterWithSniffer());
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

  private static SpanProcessor resolveDummyExporterWithSniffer() {
    return create(capturingSpanExporterWrapper);
  }

  public static ContextPropagators getPropagator() {
    return PROPAGATOR;
  }

  private static SpanProcessor resolveExporterProcessor(
                                                        SpanExporterConfiguration spanExporterConfiguration)
      throws SpanExporterConfiguratorException {

    int batchSize = parseInt(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_MAX_SIZE, "512"));

    if (batchSize < 512) {
      throw new SpanExporterConfiguratorException("The batch max size cannot be lower than 512");
    }

    int batchMaxQueueSize =
        parseInt(spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_MAX_QUEUE_SIZE, "2048"));

    return builder(createExporter(spanExporterConfiguration))
        .setMaxQueueSize(batchMaxQueueSize)
        .setMaxExportBatchSize(batchSize).build();
  }

  private static SpanExporter createExporter(SpanExporterConfiguration spanExporterConfiguration)
      throws SpanExporterConfiguratorException {
    String type = spanExporterConfiguration.getValue(MULE_OPEN_TELEMETRY_EXPORTER_TYPE);

    if (type == null) {
      throw new SpanExporterConfiguratorException("A type for span export was not created");
    }

    try {
      return valueOf(type).getSpanExporterConfigurator().configExporter(spanExporterConfiguration);
    } catch (Exception e) {
      throw new SpanExporterConfiguratorException(e);
    }
  }

  /**
   * A dummy span exporter.
   */
  private static class DummySpanExporter implements SpanExporter {

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
  }
}
