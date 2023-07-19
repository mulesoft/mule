/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.metrics.impl;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_METRICS_PROVIDER;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.exporter.api.MeterExporterFactory;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_METRICS_PROVIDER)
public class DefaultMeterProviderTestCase {

  @Test
  public void testBuilderWithoutMeterRepositoryWithoutDescription() {
    String meterName = "test-meter";
    String meterDescription = "Test Meter";
    DefaultMeterProvider defaultMeterProvider = new DefaultMeterProvider();
    MuleContext muleContext = mock(MuleContext.class);
    MeterExporterFactory muterExporterFactory = mock(MeterExporterFactory.class);
    defaultMeterProvider.muleContext = muleContext;
    defaultMeterProvider.meterExporterFactory = muterExporterFactory;
    MeterExporter meterExporter = mock(MeterExporter.class);
    when(muterExporterFactory.getMeterExporter(any())).thenReturn(meterExporter);
    Meter meter = defaultMeterProvider.getMeterBuilder(meterName).withDescription(meterDescription).build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), equalTo(meterDescription));
    assertThat(defaultMeterProvider.getMeterRepository().get(meterName).getName(), equalTo(meterName));
    verify(meterExporter).registerMeterToExport(meter);
  }

  @Test
  public void testBuilderWithoutMeterRepositoryWithDescription() {
    String meterName = "test-meter";
    String meterDescription = "Test Meter";
    DefaultMeterProvider defaultMeterProvider = new DefaultMeterProvider();
    MuleContext muleContext = mock(MuleContext.class);
    MeterExporterFactory muterExporterFactory = mock(MeterExporterFactory.class);
    defaultMeterProvider.muleContext = muleContext;
    defaultMeterProvider.meterExporterFactory = muterExporterFactory;
    MeterExporter meterExporter = mock(MeterExporter.class);
    when(muterExporterFactory.getMeterExporter(any())).thenReturn(meterExporter);
    Meter meter = defaultMeterProvider
        .getMeterBuilder(meterName)
        .withDescription(meterDescription)
        .build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), equalTo(meterDescription));
    assertThat(defaultMeterProvider.getMeterRepository().get(meterName).getName(), equalTo(meterName));
    verify(meterExporter).registerMeterToExport(meter);
  }

  @Test
  public void testLongCounterBuilder() {
    String meterName = "test-meter";
    String meterDescription = "Test Meter";
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    DefaultMeterProvider defaultMeterProvider = new DefaultMeterProvider();
    MuleContext muleContext = mock(MuleContext.class);
    MeterExporterFactory muterExporterFactory = mock(MeterExporterFactory.class);
    defaultMeterProvider.muleContext = muleContext;
    defaultMeterProvider.meterExporterFactory = muterExporterFactory;
    MeterExporter meterExporter = mock(MeterExporter.class);
    when(muterExporterFactory.getMeterExporter(any())).thenReturn(meterExporter);
    Meter meter = defaultMeterProvider.getMeterBuilder(meterName).withDescription(meterDescription).build();
    LongCounter longCounter = meter.counterBuilder(instrumentName).withDescription(instrumentDescription)
        .withUnit(unit).build();
    assertThat(longCounter.getValueAsLong(), equalTo(0L));
    longCounter.add(10L);
    assertThat(longCounter.getValueAsLong(), equalTo(10L));
    longCounter.add(5L);
    assertThat(longCounter.getValueAsLong(), equalTo(15L));
    verify(meterExporter).registerMeterToExport(meter);
  }

  @Test
  public void testLongUpDownCounterBuilder() {
    String meterName = "test-meter";
    String meterDescription = "Test Meter";
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    long initialValue = 50L;
    DefaultMeterProvider defaultMeterProvider = new DefaultMeterProvider();
    MuleContext muleContext = mock(MuleContext.class);
    MeterExporterFactory muterExporterFactory = mock(MeterExporterFactory.class);
    defaultMeterProvider.muleContext = muleContext;
    defaultMeterProvider.meterExporterFactory = muterExporterFactory;
    MeterExporter meterExporter = mock(MeterExporter.class);
    when(muterExporterFactory.getMeterExporter(any())).thenReturn(meterExporter);
    Meter meter = defaultMeterProvider.getMeterBuilder(meterName).withDescription(meterDescription).build();
    LongUpDownCounter longUpDownCounter = meter.upDownCounterBuilder(instrumentName)
        .withInitialValue(initialValue)
        .withDescription(instrumentDescription)
        .withUnit(unit).build();
    assertThat(longUpDownCounter.getValueAsLong(), equalTo(50L));
    longUpDownCounter.add(10L);
    assertThat(longUpDownCounter.getValueAsLong(), equalTo(60L));
    longUpDownCounter.add(-5L);
    assertThat(longUpDownCounter.getValueAsLong(), equalTo(55L));
    verify(meterExporter).registerMeterToExport(meter);
  }


}
