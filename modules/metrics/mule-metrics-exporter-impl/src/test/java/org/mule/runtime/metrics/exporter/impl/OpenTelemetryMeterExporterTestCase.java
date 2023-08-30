/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.exporter.impl;

import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_ENABLED;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_INTERVAL;
import static org.mule.runtime.metrics.exporter.config.api.OpenTelemetryMeterExporterConfigurationProperties.MULE_OPEN_TELEMETRY_METER_EXPORTER_TYPE;
import static org.mule.runtime.metrics.exporter.impl.config.OpenTelemetryMeterExporterTransport.IN_MEMORY;
import static org.mule.runtime.metrics.exporter.impl.OpenTelemetryMeterExporterFactory.METER_SNIFFER_EXPORTER;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.METRICS_EXPORTER;

import static java.lang.Boolean.TRUE;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.exporter.config.api.MeterExporterConfiguration;
import org.mule.runtime.metrics.exporter.config.impl.FileMeterExporterConfiguration;
import org.mule.runtime.metrics.exporter.impl.optel.config.OpenTelemetryAutoConfigurableMeterExporterConfiguration;
import org.mule.runtime.metrics.exporter.impl.utils.TestMeterExporterConfiguration;
import org.mule.runtime.metrics.exporter.impl.utils.TestOpenTelemetryMeterExporterFactory;
import org.mule.runtime.metrics.impl.meter.DefaultMeter;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(METRICS_EXPORTER)
public class OpenTelemetryMeterExporterTestCase {

  private static final String METER_EXPORTER_INTERVAL = "1";
  private static final String METER_NAME = "testMetricName";
  private static final String LONG_COUNTER_NAME = "long-counter-test";
  private static final String LONG_COUNTER_DESCRIPTION = "Long Counter test";
  private static final String UNIT_NAME = "test-unit";
  private static final String LONG_UP_DOWN_COUNTER_NAME = "long-up-down-counter-test";
  private static final String LONG_UP_DOWN_COUNTER_DESCRIPTION = "Long UpDownCounter test";
  private static final long LONG_UP_DOWN_INITIAL_VALUE = 50L;
  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  private MeterExporterConfiguration configuration;
  private MeterExporterConfiguration fileMeterExporterConfiguration;
  private Meter meter;
  private LongCounter longCounter;
  private LongUpDownCounter longUpDownCounter;

  @Before
  public void setUp() {
    Map<String, String> properties = getMeterExporterProperties();
    configuration = getMeterExporterConfiguration(properties);

    MuleContext muleContext = mock(MuleContext.class);
    fileMeterExporterConfiguration = new TestFileMeterExporterConfiguration(muleContext);

    MeterExporter meterExporter = mock(MeterExporter.class);
    meter = DefaultMeter.builder(METER_NAME)
        .withMeterExporter(meterExporter)
        .build();

    longCounter = meter.counterBuilder(LONG_COUNTER_NAME)
        .withDescription(LONG_COUNTER_DESCRIPTION)
        .withUnit(UNIT_NAME)
        .build();

    longUpDownCounter = meter.upDownCounterBuilder(LONG_UP_DOWN_COUNTER_NAME)
        .withInitialValue(LONG_UP_DOWN_INITIAL_VALUE)
        .withDescription(LONG_UP_DOWN_COUNTER_DESCRIPTION)
        .withUnit(UNIT_NAME)
        .build();
  }

  @Test
  public void exporterShouldExportLongCounterMetricSuccessfully() {
    OpenTelemetryMeterExporterFactory openTelemetryMeterExporterFactory = new TestOpenTelemetryMeterExporterFactory();
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
    OpenTelemetryMeterExporterFactory openTelemetryMeterExporterFactory = new TestOpenTelemetryMeterExporterFactory();
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

  @Test
  public void exporterWithAFileConfigShouldExportLongCounterMetricSuccessfully() {
    OpenTelemetryMeterExporterFactory openTelemetryMeterExporterFactory = new TestOpenTelemetryMeterExporterFactory();
    MeterExporter openTelemetryMeterExporter = openTelemetryMeterExporterFactory.getMeterExporter(fileMeterExporterConfiguration);
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
  public void exporterWithAFileConfigShouldExportUpDownCounterMetricSuccessfully() {
    OpenTelemetryMeterExporterFactory openTelemetryMeterExporterFactory = new TestOpenTelemetryMeterExporterFactory();
    MeterExporter openTelemetryMeterExporter = openTelemetryMeterExporterFactory.getMeterExporter(fileMeterExporterConfiguration);
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

  @NotNull
  private static Map<String, String> getMeterExporterProperties() {
    Map<String, String> properties = new HashMap<>();
    properties.put(MULE_OPEN_TELEMETRY_METER_EXPORTER_ENABLED, TRUE.toString());
    properties.put(MULE_OPEN_TELEMETRY_METER_EXPORTER_TYPE, IN_MEMORY.name());
    properties.put(MULE_OPEN_TELEMETRY_METER_EXPORTER_INTERVAL, METER_EXPORTER_INTERVAL);
    return properties;
  }

  @NotNull
  private static OpenTelemetryAutoConfigurableMeterExporterConfiguration getMeterExporterConfiguration(Map<String, String> properties) {
    return new OpenTelemetryAutoConfigurableMeterExporterConfiguration(new TestMeterExporterConfiguration(properties));
  }

  private List<MetricData> getMetricsByCounterName(List<MetricData> metrics, String metricName) {
    return metrics.stream().filter(metric -> metric.getName().equals(metricName)).collect(Collectors.toList());
  }

  private static class TestFileMeterExporterConfiguration extends FileMeterExporterConfiguration {

    public static final String CONF_FOLDER = "conf";

    /**
     * {@link FileMeterExporterConfiguration} used for testing properties file.
     */
    public TestFileMeterExporterConfiguration(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    protected ClassLoader getExecutionClassLoader(MuleContext muleContext) {
      return Thread.currentThread().getContextClassLoader();
    }

    @Override
    protected String getConfFolder() {
      return CONF_FOLDER;
    }
  }
}
