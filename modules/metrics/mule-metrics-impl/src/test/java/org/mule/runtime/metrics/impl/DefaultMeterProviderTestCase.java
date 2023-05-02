/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CONFIGURATION;

import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.meter.Meter;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(TRACING_CONFIGURATION)
public class DefaultMeterProviderTestCase {

  @Test
  public void testBuilderWithoutMeterRepositoryWithoutDescription() {
    String meterName = "test-meter";
    String meterDescription = "Test Meter";
    DefaultMeterProvider defaultMeterProvider = new DefaultMeterProvider();
    Meter meter = defaultMeterProvider.getMeterBuilder(meterName).withDescription(meterDescription).build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), equalTo(meterDescription));
    assertThat(defaultMeterProvider.getMeterRepository().get(meterName).getName(), equalTo(meterName));
  }

  @Test
  public void testBuilderWithoutMeterRepositoryWithDescription() {
    String meterName = "test-meter";
    String meterDescription = "Test Meter";
    DefaultMeterProvider defaultMeterProvider = new DefaultMeterProvider();
    Meter meter = defaultMeterProvider.getMeterBuilder(meterName).withDescription(meterDescription).build();
    assertThat(meter.getName(), equalTo(meterName));
    assertThat(meter.getDescription(), equalTo(meterDescription));
    assertThat(defaultMeterProvider.getMeterRepository().get(meterName).getName(), equalTo(meterName));
  }

  @Test
  public void testLongCounterBuilder() {
    String meterName = "test-meter";
    String meterDescription = "Test Meter";
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    DefaultMeterProvider defaultMeterProvider = new DefaultMeterProvider();
    Meter meter = defaultMeterProvider.getMeterBuilder(meterName).withDescription(meterDescription).build();
    LongCounter longCounter = meter.counterBuilder(instrumentName).withDescription(instrumentDescription)
        .withUnit(unit).build();
    assertThat(longCounter.getValue(), equalTo(0L));
    longCounter.add(10L);
    assertThat(longCounter.getValue(), equalTo(10L));
    longCounter.add(5L);
    assertThat(longCounter.getValue(), equalTo(15L));
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
    Meter meter = defaultMeterProvider.getMeterBuilder(meterName).withDescription(meterDescription).build();
    LongUpDownCounter longUpDownCounter = meter.upDownCounterBuilder(instrumentName).withDescription(instrumentDescription)
        .withUnit(unit).withInitialValue(initialValue).build();
    assertThat(longUpDownCounter.getValue(), equalTo(50L));
    longUpDownCounter.add(10L);
    assertThat(longUpDownCounter.getValue(), equalTo(60L));
    longUpDownCounter.add(-5L);
    assertThat(longUpDownCounter.getValue(), equalTo(55L));
  }
}
