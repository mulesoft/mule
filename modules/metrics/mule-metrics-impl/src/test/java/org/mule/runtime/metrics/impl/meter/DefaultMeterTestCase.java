/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.meter;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.METRICS_IMPLEMENTATION;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.emptyOrNullString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.exporter.api.MeterExporter;
import org.mule.runtime.metrics.impl.meter.repository.MeterRepository;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(PROFILING)
@Story(METRICS_IMPLEMENTATION)
public class DefaultMeterTestCase {

  @Test
  public void testBuilderWithoutMeterRepositoryWithoutDescription() {
    final String meterName = "test-meter";
    final MeterExporter meterExporter = mock(MeterExporter.class);
    final Meter meter = DefaultMeter
        .builder(meterName)
        .withMeterExporter(meterExporter)
        .build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), is(emptyOrNullString()));
  }

  @Test
  public void testBuilderWithoutMeterRepositoryWithDescription() {
    final String meterName = "test-meter";
    final String testEmptyString = "Test empty string";
    final MeterExporter meterExporter = mock(MeterExporter.class);
    final Meter meter = DefaultMeter.builder(meterName)
        .withMeterExporter(meterExporter)
        .withDescription(testEmptyString)
        .build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), equalTo(testEmptyString));
  }

  @Test
  public void testBuilderWithMeterRepositoryWithoutDescription() {
    final String meterName = "test-meter";
    final MeterRepository meterRepository = mock(MeterRepository.class);
    final MeterExporter meterExporter = mock(MeterExporter.class);
    final Meter meter = DefaultMeter.builder(meterName)
        .withMeterExporter(meterExporter)
        .withMeterRepository(meterRepository)
        .build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), is(emptyOrNullString()));
    verify(meterRepository).getOrCreate(eq(meterName), any());
  }

  @Test
  public void testBuilderWithMeterRepositoryWithDescription() {
    final String meterName = "test-meter";
    final String testEmptyString = "Test empty string";
    final MeterRepository meterRepository = mock(MeterRepository.class);
    final MeterExporter meterExporter = mock(MeterExporter.class);
    final Meter meter = DefaultMeter.builder(meterName)
        .withMeterExporter(meterExporter)
        .withMeterRepository(meterRepository)
        .withDescription(testEmptyString)
        .build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), equalTo(testEmptyString));
    verify(meterRepository).getOrCreate(eq(meterName), any());
  }

}
