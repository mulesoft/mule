/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.exporter.api.DummyConfiguration;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.exporter.impl.config.OpenTelemetryMeterExporterTransport;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;

public class OpenTelemetryMeterExporter implements MeterExporter {

  private MeterProvider meterProvider;
  private Map<String, Meter> meters = new HashMap<>();

  public OpenTelemetryMeterExporter(DummyConfiguration configuration) {
    MetricExporter metricExporter =
        OpenTelemetryMeterExporterTransport.valueOf(configuration.getExporterType()).getMetricExporter();
    this.meterProvider = SdkMeterProvider.builder()
        .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
            .setInterval(configuration.getExportingInterval(), SECONDS).build())
        .build();
  }

  @Override
  public void registerInstrumentForExport(LongCounter longCounter) {
    Meter meter = meters.get(longCounter.getMeterName());
    meter.counterBuilder(longCounter.getName()).buildWithCallback(measurement -> measurement.record(longCounter.getValue()));
  }

  @Override
  public void registerInstrumentForExport(LongUpDownCounter upDownCounter) {
    Meter meter = meters.get(upDownCounter.getMeterName());
    meter.upDownCounterBuilder(upDownCounter.getName())
        .buildWithCallback(measurement -> measurement.record(upDownCounter.getValue()));
  }

  @Override
  public void registerMeterForExport(org.mule.runtime.metrics.api.meter.Meter meter) {
    meters.put(meter.getName(), meterProvider.meterBuilder(meter.getName()).build());
  }
}
