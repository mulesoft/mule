/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.exporter.impl.capturer;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;

/**
 * A {@link MetricExporter} that captures OpenTelemetry exported metrics.
 *
 * @since 4.5.0
 */
public class CapturingMeterExporterWrapper implements MetricExporter {

  private final Set<InMemoryMetricExporter> meterSniffers = ConcurrentHashMap.newKeySet();

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    meterSniffers.forEach(sniffer -> sniffer.export(metrics));
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return null;
  }

  @Override
  public CompletableResultCode shutdown() {
    return null;
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return AggregationTemporality.CUMULATIVE;
  }

  public InMemoryMetricExporter getExportedMeterSniffer() {
    InMemoryMetricExporter meterSniffer = InMemoryMetricExporter.create();
    meterSniffers.add(meterSniffer);
    return meterSniffer;
  }

  public void dispose(InMemoryMetricExporter meterSniffer) {
    meterSniffers.remove(meterSniffer);
  }
}
