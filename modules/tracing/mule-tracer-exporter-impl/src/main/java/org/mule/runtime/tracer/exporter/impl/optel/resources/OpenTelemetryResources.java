/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.exporter.impl.optel.resources;

import static java.lang.System.getProperty;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SCHEDULED_DELAY;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_METRICS_LOG_FREQUENCY;
import static org.mule.runtime.tracer.exporter.config.api.OpenTelemetrySpanExporterConfigurationProperties.MULE_OPEN_TELEMETRY_EXPORTER_TYPE;
import static org.mule.runtime.tracer.exporter.impl.config.type.OpenTelemetryExporterTransport.valueOf;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static java.time.Duration.ofMillis;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.context.propagation.ContextPropagators.create;
import static io.opentelemetry.sdk.resources.Resource.getDefault;
import static io.opentelemetry.sdk.trace.export.BatchSpanProcessor.builder;

import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.mule.runtime.tracer.exporter.config.api.SpanExporterConfiguration;
import org.mule.runtime.tracer.exporter.impl.metrics.OpenTelemetryExportQueueMetrics;

import java.util.Collection;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Retriever open telemetry resources.
 *
 * @since 4.5.0
 */
public class OpenTelemetryResources {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenTelemetryResources.class);

  private OpenTelemetryResources() {}

  private static final ContextPropagators PROPAGATOR = create(W3CTraceContextPropagator.getInstance());

  // This is only defined in the semconv artifact which is in alpha state and is only needed for this.
  // In order not to add another dependency we add it here.
  // For the moment it is defined in the spec here:
  // TODO: W-11610439: tracking: verify if the semconv dependency (alpha) should be added
  // https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/resource/semantic_conventions/README.md#semantic-attributes-with-dedicated-environment-variable
  public static final AttributeKey<String> SERVICE_NAME_KEY = stringKey("service.name");

  private static SdkMeterProvider getMeterProvider(SpanExporterConfiguration spanExporterConfiguration) {
    MetricReader periodicReader =
        PeriodicMetricReader.builder(new OpenTelemetryExportQueueMetrics())
            .setInterval(ofMillis(parseLong(spanExporterConfiguration
                .getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_METRICS_LOG_FREQUENCY))))
            .build();
    return SdkMeterProvider.builder().registerMetricReader(periodicReader).build();
  }

  public static Resource getResource(String serviceName) {
    return getDefault().merge(Resource.create(Attributes.of(SERVICE_NAME_KEY, serviceName)));
  }

  public static ContextPropagators getPropagator() {
    return PROPAGATOR;
  }

  public static SpanProcessor resolveOpenTelemetrySpanProcessor(SpanExporterConfiguration spanExporterConfiguration,
                                                                SpanExporterConfiguration privilegedSpanExporterConfiguration,
                                                                SpanExporter spanExporter)
      throws SpanExporterConfiguratorException {

    LOGGER.debug("Mule Open Telemetry Tracer Exporter Endpoint is {}",
                 spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_ENDPOINT));
    LOGGER.debug("Mule Open Telemetry Tracer Exporter Protocol Type is {}",
                 spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_TYPE));

    int maxBatchSize = parseInt(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_MAX_BATCH_SIZE));

    if (maxBatchSize < 512) {
      throw new SpanExporterConfiguratorException("The batch max size cannot be lower than 512");
    }

    int batchQueueSize =
        parseInt(spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_QUEUE_SIZE));

    return builder(spanExporter)
        .setMaxQueueSize(batchQueueSize)
        .setMeterProvider(getMeterProvider(spanExporterConfiguration))
        .setScheduleDelay(parseLong(privilegedSpanExporterConfiguration
            .getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_BATCH_SCHEDULED_DELAY)), MILLISECONDS)
        .setMaxExportBatchSize(maxBatchSize).build();
  }

  public static SpanExporter resolveOpenTelemetrySpanExporter(SpanExporterConfiguration spanExporterConfiguration)
      throws SpanExporterConfiguratorException {

    String type = spanExporterConfiguration.getStringValue(MULE_OPEN_TELEMETRY_EXPORTER_TYPE);

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
  public static class NoOpSpanExporter implements SpanExporter {

    public static SpanExporter getInstance() {
      return new NoOpSpanExporter();
    }

    private NoOpSpanExporter() {}

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
