/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl;

import static org.mule.runtime.metrics.exporter.impl.OpenTelemetryMeterExporterFactory.METER_SNIFFER_EXPORTER;
import static org.mule.runtime.metrics.exporter.impl.config.OpenTelemetryMeterExporterTransport.IN_MEMORY;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.exporter.api.DummyConfiguration;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.impl.meter.DefaultMeter;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.List;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import org.junit.Before;
import org.junit.Test;

public class OpenTelemetryMeterExporterTestCase {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  private DummyConfiguration configuration;
  private Meter meter;
  private LongCounter longCounter;
  private LongUpDownCounter longUpDownCounter;

  @Before
  public void setUp() {
    configuration = new DummyConfiguration() {

      @Override
      public String getExporterType() {
        return IN_MEMORY.name();
      }

      @Override
      public Integer getExportingInterval() {
        return 1;
      }
    };

    meter = DefaultMeter.builder("testMetricName").build();
    longCounter = meter.counterBuilder("long-counter-test").withDescription("Long Counter test")
        .withUnit("test-unit").build();
    longUpDownCounter = meter.upDownCounterBuilder("long-up-down-counter-test").withDescription("Long UpDownCounter test")
        .withUnit("test-unit").withInitialValue(50L).build();
  }

  @Test
  public void exporterShouldExportLongCounterMetricSuccessfully() {
    OpenTelemetryMeterExporterFactory openTelemetryMeterExporterFactory = new OpenTelemetryMeterExporterFactory();
    MeterExporter openTelemetryMeterExporter = openTelemetryMeterExporterFactory.getMeterExporter(configuration);
    InMemoryMetricExporter inMemoryMetricExporter = METER_SNIFFER_EXPORTER.getExportedMeterSniffer();

    try {
      openTelemetryMeterExporter.registerMeterToExport(meter);
      openTelemetryMeterExporter.enableExport(longCounter);
      longCounter.add(4);

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          return getMetricsByCounterName(inMemoryMetricExporter.getFinishedMetricItems(), longCounter.getName())
              .size() >= 1;
        }

        @Override
        public String describeFailure() {
          return "The expected amount of metrics was not captured";
        }
      });

      getMetricsByCounterName(inMemoryMetricExporter.getFinishedMetricItems(), longCounter.getName()).get(0)
          .getLongSumData().getPoints().stream()
          .forEach(longPointData -> assertThat(longPointData.getValue(), equalTo(4L)));
    } finally {
      METER_SNIFFER_EXPORTER.dispose(inMemoryMetricExporter);
    }
  }

  @Test
  public void exporterShouldExportUpDownCounterMetricSuccessfully() {
    OpenTelemetryMeterExporterFactory openTelemetryMeterExporterFactory = new OpenTelemetryMeterExporterFactory();
    MeterExporter openTelemetryMeterExporter = openTelemetryMeterExporterFactory.getMeterExporter(configuration);
    InMemoryMetricExporter inMemoryMetricExporter = METER_SNIFFER_EXPORTER.getExportedMeterSniffer();

    try {
      openTelemetryMeterExporter.registerMeterToExport(meter);
      openTelemetryMeterExporter.enableExport(longUpDownCounter);
      longUpDownCounter.add(4);
      longUpDownCounter.add(-1);

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          return getMetricsByCounterName(inMemoryMetricExporter.getFinishedMetricItems(), longUpDownCounter.getName())
              .size() >= 1;
        }

        @Override
        public String describeFailure() {
          return "The expected amount of metrics was not captured";
        }
      });

      getMetricsByCounterName(inMemoryMetricExporter.getFinishedMetricItems(), longUpDownCounter.getName())
          .get(0).getLongSumData().getPoints().stream()
          .forEach(longPointData -> assertThat(longPointData.getValue(), equalTo(53L)));
    } finally {
      METER_SNIFFER_EXPORTER.dispose(inMemoryMetricExporter);
    }
  }

  private List<MetricData> getMetricsByCounterName(List<MetricData> metrics, String name) {
    return metrics.stream()
        .filter(metricData -> metricData.getName().equals(name)).collect(Collectors.toList());
  }

}
