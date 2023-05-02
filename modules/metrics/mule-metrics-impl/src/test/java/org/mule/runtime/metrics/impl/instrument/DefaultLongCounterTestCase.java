/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metrics.impl.instrument;

import static org.mule.runtime.metrics.impl.instrument.DefaultLongCounter.builder;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.TRACING_CONFIGURATION;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.rules.ExpectedException.none;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mule.runtime.metrics.api.instrument.LongCounter;
import org.mule.runtime.metrics.impl.instrument.repository.InstrumentRepository;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(TRACING_CONFIGURATION)
public class DefaultLongCounterTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void testWithoutInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    LongCounter longCounter = builder(instrumentName).withDescription(instrumentDescription).withUnit(unit).build();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));

    // Verify counter.
    verifyCounterValues(longCounter);
  }

  @Test
  public void testWithInstrumentRepository() {
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    InstrumentRepository repository = mock(InstrumentRepository.class);
    LongCounter longCounter = builder(instrumentName).withInstrumentRepository(repository).withDescription(instrumentDescription)
        .withUnit(unit).build();
    assertThat(longCounter.getName(), equalTo(instrumentName));
    assertThat(longCounter.getDescription(), equalTo(instrumentDescription));
    assertThat(longCounter.getUnit(), equalTo(unit));
    verify(repository).register(eq(instrumentName), any());

    // Verify counter.
    verifyCounterValues(longCounter);
  }

  @Test
  public void testAddingInvalidValue() {
    expectedException.expect(IllegalArgumentException.class);
    String instrumentName = "long-counter-test";
    String instrumentDescription = "Long Counter test";
    String unit = "test-unit";
    LongCounter longCounter = builder(instrumentName).withDescription(instrumentDescription)
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
