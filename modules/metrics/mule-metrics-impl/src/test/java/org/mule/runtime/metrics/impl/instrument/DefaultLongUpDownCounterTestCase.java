/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument;

import static org.mule.runtime.metrics.impl.instrument.DefaultLongUpDownCounter.builder;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.METRICS_IMPLEMENTATION;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.metrics.api.instrument.LongUpDownCounter;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

import java.util.Arrays;
import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(METRICS_IMPLEMENTATION)
@RunWith(Parameterized.class)
public class DefaultLongUpDownCounterTestCase {

  private final long initialValue;

  @Parameterized.Parameters(name = "initialValue: {0}")
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {
        {0},
        {50},
        {-50}
    });
  }

  public DefaultLongUpDownCounterTestCase(long initialValue) {
    this.initialValue = initialValue;
  }

  @Test
  public void testWithoutInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    Meter meter = mock(Meter.class);
    when(meter.getName()).thenReturn(meterName);
    LongUpDownCounter longCounter =
        builder(instrumentName, meter).withDescription(instrumentDescription).withUnit(unit).withInitialValue(initialValue)
            .build();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));
    assertThat(longCounter.getMeter().getName(), equalTo(meterName));

    // Verify counter.
    verifyCounterValues(longCounter, initialValue);
  }

  @Test
  public void testWithInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    InstrumentRepository repository = mock(InstrumentRepository.class);
    Meter meter = mock(Meter.class);
    when(meter.getName()).thenReturn(meterName);
    LongUpDownCounter longCounter =
        builder(instrumentName, meter).withInstrumentRepository(repository).withDescription(instrumentDescription)
            .withInitialValue(initialValue)
            .withUnit(unit).build();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));
    assertThat(longCounter.getMeter().getName(), equalTo(meterName));
    verify(repository).create(eq(instrumentName), any());

    // Verify counter.
    verifyCounterValues(longCounter, initialValue);
  }

  private static void verifyCounterValues(LongUpDownCounter longCounter, long initialValue) {
    assertThat(longCounter.getValueAsLong(), equalTo(initialValue));
    longCounter.add(10l);
    assertThat(longCounter.getValueAsLong(), equalTo(initialValue + 10));
    longCounter.add(5l);
    assertThat(longCounter.getValueAsLong(), equalTo(initialValue + 15));
    longCounter.add(-5l);
    assertThat(longCounter.getValueAsLong(), equalTo(initialValue + 10));
  }
}
