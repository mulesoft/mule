/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.meter;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.METRICS_IMPLEMENTATION;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.impl.meter.repository.MeterRepository;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(METRICS_IMPLEMENTATION)
public class DefaultMeterTestCase {

  @Test
  public void testBuilderWithoutMeterRepositoryWithoutDescription() {
    String meterName = "test-meter";
    MeterExporter meterExporter = mock(MeterExporter.class);
    Meter meter = DefaultMeter
        .builder(meterName)
        .withMeterExporter(meterExporter)
        .build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), isEmptyOrNullString());
  }

  @Test
  public void testBuilderWithoutMeterRepositoryWithDescription() {
    String meterName = "test-meter";
    String testEmptyString = "Test empty string";
    MeterExporter meterExporter = mock(MeterExporter.class);
    Meter meter = DefaultMeter.builder(meterName)
        .withMeterExporter(meterExporter)
        .withDescription(testEmptyString)
        .build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), equalTo(testEmptyString));
  }

  @Test
  public void testBuilderWithMeterRepositoryWithoutDescription() {
    String meterName = "test-meter";
    MeterRepository meterRepository = mock(MeterRepository.class);
    MeterExporter meterExporter = mock(MeterExporter.class);
    Meter meter = DefaultMeter.builder(meterName)
        .withMeterExporter(meterExporter)
        .withMeterRepository(meterRepository)
        .build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), isEmptyOrNullString());
    verify(meterRepository).getOrCreate(eq(meterName), any());
  }

  @Test
  public void testBuilderWithMeterRepositoryWithDescription() {
    String meterName = "test-meter";
    String testEmptyString = "Test empty string";
    MeterRepository meterRepository = mock(MeterRepository.class);
    MeterExporter meterExporter = mock(MeterExporter.class);
    Meter meter = DefaultMeter.builder(meterName)
        .withMeterExporter(meterExporter)
        .withMeterRepository(meterRepository)
        .withDescription(testEmptyString)
        .build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), equalTo(testEmptyString));
    verify(meterRepository).getOrCreate(eq(meterName), any());
  }

}
