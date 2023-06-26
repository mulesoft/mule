/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument;

import static org.mule.runtime.metrics.impl.instrument.DefaultLongCounter.builder;
import static org.mule.runtime.metrics.impl.instrument.ValueSupplierBasedLongCounter.getValueSupplierBasedLongCounterRegistrationWithInstrumentRepositoryHelper;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.METRICS_IMPLEMENTATION;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.api.meter.Meter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

import java.util.concurrent.atomic.AtomicLong;

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
  public void testBuildWithoutInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    Meter meter = mock(Meter.class);
    when(meter.getName()).thenReturn(meterName);
    LongCounter longCounter = builder(instrumentName, meter).withDescription(instrumentDescription).withUnit(unit).build();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));
    assertThat(longCounter.getMeter().getName(), equalTo(meterName));

    // Verify counter.
    verifyCounterValues(longCounter);
  }

  @Test
  public void testRegistrationWithoutInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    Meter meter = mock(Meter.class);
    when(meter.getName()).thenReturn(meterName);
    AtomicLong value = new AtomicLong(0);
    LongCounter longCounter =
        getValueSupplierBasedLongCounterRegistrationWithInstrumentRepositoryHelper(instrumentName, meter)
            .withDescription(instrumentDescription)
            .withUnit(unit)
            .withSupplierForIncrementAndGetOperation(value::incrementAndGet)
            .withConsumerForAddOperation(value::addAndGet)
            .withValueSupplier(value::get)
            .register();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));
    assertThat(longCounter.getMeter().getName(), equalTo(meterName));

    // Verify counter.
    verifyCounterValues(longCounter);
  }

  @Test
  public void testBuildWithInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    InstrumentRepository repository = new TestRepository();
    Meter meter = mock(Meter.class);
    when(meter.getName()).thenReturn(meterName);
    LongCounter longCounter =
        builder(instrumentName, meter).withInstrumentRepository(repository).withDescription(instrumentDescription)
            .withUnit(unit).build();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));
    assertThat(longCounter.getMeter().getName(), equalTo(meterName));

    // Verify counter.
    verifyCounterValues(longCounter);
  }

  @Test
  public void testRegisterWithInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    Meter meter = mock(Meter.class);
    when(meter.getName()).thenReturn(meterName);
    InstrumentRepository repository = new TestRepository();
    AtomicLong value = new AtomicLong(0);
    LongCounter longCounter =
        getValueSupplierBasedLongCounterRegistrationWithInstrumentRepositoryHelper(instrumentName, meter)
            .withDescription(instrumentDescription)
            .withUnit(unit)
            .withSupplierForIncrementAndGetOperation(value::incrementAndGet)
            .withConsumerForAddOperation(value::addAndGet)
            .withValueSupplier(value::get)
            .withInstrumentRepository(repository)
            .register();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));
    assertThat(longCounter.getMeter().getName(), equalTo(meterName));

    // Verify counter.
    verifyCounterValues(longCounter);
  }

  @Test
  public void testBuildAddingInvalidValue() {
    expectedException.expect(IllegalArgumentException.class);
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    Meter meter = mock(Meter.class);
    when(meter.getName()).thenReturn(meterName);
    LongCounter longCounter = builder(instrumentName, meter).withDescription(instrumentDescription)
        .withUnit(unit).build();
    longCounter.add(-10);
  }

  @Test
  public void testRegisterAddingInvalidValue() {
    expectedException.expect(IllegalArgumentException.class);
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    String meterName = "test-meter";
    AtomicLong value = new AtomicLong(0);
    Meter meter = mock(Meter.class);
    LongCounter longCounter =
        getValueSupplierBasedLongCounterRegistrationWithInstrumentRepositoryHelper(instrumentName, meter)
            .withDescription(instrumentDescription)
            .withUnit(unit)
            .withSupplierForIncrementAndGetOperation(value::incrementAndGet)
            .withConsumerForAddOperation(value::addAndGet)
            .withValueSupplier(value::get)
            .register();
    longCounter.add(-10);
  }

  private static void verifyCounterValues(LongCounter longCounter) {
    assertThat(longCounter.getValueAsLong(), equalTo(0L));
    longCounter.add(10L);
    assertThat(longCounter.getValueAsLong(), equalTo(10L));
    longCounter.add(5L);
    assertThat(longCounter.getValueAsLong(), equalTo(15L));
  }

  private class TestRepository extends InstrumentRepository {
  }
}
