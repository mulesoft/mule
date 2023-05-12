/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument;

import static org.mule.runtime.metrics.impl.instrument.DefaultLongCounter.builder;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.METRICS_IMPLEMENTATION;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(PROFILING)
@Story(METRICS_IMPLEMENTATION)
public class DefaultLongCounterTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void testWithoutInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    LongCounter longCounter = builder(instrumentName, meterName).withDescription(instrumentDescription).withUnit(unit).build();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));
    assertThat(longCounter.getMeterName(), equalTo(meterName));

    // Verify counter.
    verifyCounterValues(longCounter);
  }

  @Test
  public void testWithInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    InstrumentRepository repository = mock(InstrumentRepository.class);
    LongCounter longCounter =
        builder(instrumentName, meterName).withInstrumentRepository(repository).withDescription(instrumentDescription)
            .withUnit(unit).build();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));
    assertThat(longCounter.getMeterName(), equalTo(meterName));
    verify(repository).create(eq(instrumentName), any());

    // Verify counter.
    verifyCounterValues(longCounter);
  }

  @Test
  public void testAddingInvalidValue() {
    expectedException.expect(IllegalArgumentException.class);
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    LongCounter longCounter = builder(instrumentName, meterName).withDescription(instrumentDescription)
        .withUnit(unit).build();
    longCounter.add(-10);
  }

  private static void verifyCounterValues(LongCounter longCounter) {
    assertThat(longCounter.getValue(), equalTo(0L));
    longCounter.add(10L);
    assertThat(longCounter.getValue(), equalTo(10L));
    longCounter.add(5L);
    assertThat(longCounter.getValue(), equalTo(15L));
  }
}
