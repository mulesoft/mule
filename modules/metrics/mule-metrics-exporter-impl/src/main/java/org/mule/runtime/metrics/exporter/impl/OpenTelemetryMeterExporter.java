/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl;

import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_INTERVAL;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_TYPE;
import static org.mule.runtime.metrics.exporter.impl.config.OpenTelemetryMeterExporterTransport.valueOf;

import static java.lang.Long.parseLong;
import static java.util.concurrent.TimeUnit.SECONDS;

import io.opentelemetry.api.common.AttributesBuilder;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;
import org.mule.runtime.metrics.exporter.impl.optel.resources.MeterExporterConfiguratorException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounterBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.ObservableLongCounter;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

/**
 * A {@link MeterExporter} that exports metrics using open telemetry.
 *
 * @since 4.5.0
 */
public class OpenTelemetryMeterExporter implements MeterExporter, Disposable {

  private final List<ObservableLongCounter> counters = new ArrayList<>();
  private final List<ObservableLongUpDownCounter> upDownCounters = new ArrayList<>();
  private final Map<String, Meter> openTelemetryMeters = new HashMap<>();
  private final MeterProvider meterProvider;
  private final PeriodicMetricReader periodicMetricReader;

  public OpenTelemetryMeterExporter(MeterExporterConfiguration configuration, Resource resource) {
    // TODO W-13218993: Finish ADR adoption and implementation.
    MetricExporter metricExporter;
    String meterExporterType = configuration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_TYPE);

    if (meterExporterType == null) {
      throw new MeterExporterConfiguratorException("A type for the metric export was not created");
    }

    try {
      metricExporter = valueOf(meterExporterType).getMeterExporterConfigurator().configExporter(configuration);
    } catch (Exception e) {
      throw new MeterExporterConfiguratorException(e);
    }

    long periodicMetricReaderInterval = parseLong(configuration.getStringValue(MULE_OPEN_TELEMETRY_METER_EXPORTER_INTERVAL));
    this.periodicMetricReader = PeriodicMetricReader.builder(metricExporter)
        .setInterval(periodicMetricReaderInterval, SECONDS).build();

    this.meterProvider = SdkMeterProvider.builder()
        .setResource(resource)
        .registerMetricReader(periodicMetricReader)
        .build();
  }

  @Override
  public synchronized void enableExport(LongCounter longCounter) {
    Meter openTelemetryMeter = openTelemetryMeters.get(longCounter.getMeter().getName());
    LongCounterBuilder longCounterBuilder =
        openTelemetryMeter.counterBuilder(longCounter.getName()).setDescription(longCounter.getDescription());
    if (longCounter.getUnit() != null) {
      longCounterBuilder = longCounterBuilder.setUnit(longCounter.getUnit());
    }
    io.opentelemetry.api.metrics.LongCounter otelLongCounter = longCounterBuilder.build();
    longCounter.onAddition((value, stringStringMap) -> otelLongCounter.add(value, getOtelAttributes(stringStringMap)));
  }

  private Attributes getOtelAttributes(Map<String, String> stringStringMap) {
    AttributesBuilder attributesBuilder = Attributes.builder();
    stringStringMap.forEach(attributesBuilder::put);
    return attributesBuilder.build();
  }

  @Override
  public synchronized void enableExport(LongUpDownCounter upDownCounter) {
    Meter openTelemetryMeter = openTelemetryMeters.get(upDownCounter.getMeter().getName());
    Attributes attributes = new OpentelemetryExporterAttributes(upDownCounter.getMeter());
    LongUpDownCounterBuilder longUpDownCounter = openTelemetryMeter.upDownCounterBuilder(upDownCounter.getName())
        .setDescription(upDownCounter.getDescription());

    if (upDownCounter.getUnit() != null) {
      longUpDownCounter = longUpDownCounter.setUnit(upDownCounter.getUnit());
    }

    upDownCounters
        .add(longUpDownCounter.buildWithCallback(measurement -> measurement.record(upDownCounter.getValueAsLong(), attributes)));
  }

  @Override
  public synchronized void registerMeterToExport(org.mule.runtime.metrics.api.meter.Meter meter) {
    openTelemetryMeters.put(meter.getName(), meterProvider.meterBuilder(meter.getName()).build());
  }

  @Override
  public void dispose() {
    counters.forEach(ObservableLongCounter::close);
    upDownCounters.forEach(ObservableLongUpDownCounter::close);

    if (periodicMetricReader != null) {
      periodicMetricReader.shutdown();
    }
  }
}
