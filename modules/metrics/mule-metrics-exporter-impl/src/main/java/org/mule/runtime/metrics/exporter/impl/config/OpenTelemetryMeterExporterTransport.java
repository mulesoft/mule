/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.metrics.exporter.impl.config;

import static org.mule.runtime.metrics.exporter.impl.OpenTelemetryMeterExporterFactory.METER_SNIFFER_EXPORTER;

import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

/**
 * Specifies different ways in which metrics can be exported.
 *
 * @since 4.5.0
 */
public enum OpenTelemetryMeterExporterTransport {

  GRPC(OtlpGrpcMetricExporter.getDefault()),

  HTTP(OtlpHttpMetricExporter.getDefault()),

  IN_MEMORY(METER_SNIFFER_EXPORTER);

  private final MetricExporter metricExporter;

  OpenTelemetryMeterExporterTransport(MetricExporter metricExporter) {
    this.metricExporter = metricExporter;
  }

  public MetricExporter getMetricExporter() {
    return metricExporter;
  }
}
