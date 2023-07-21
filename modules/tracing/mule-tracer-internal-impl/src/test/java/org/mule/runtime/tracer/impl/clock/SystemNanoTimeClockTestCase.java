/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.clock;


import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.Thread.sleep;
import static java.time.Instant.ofEpochMilli;

import static org.junit.Assert.assertTrue;

import java.time.Instant;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class SystemNanoTimeClockTestCase {

  @Test
  public void testClock() throws Exception {
    Instant instant = ofEpochMilli(System.currentTimeMillis());
    sleep(1L);
    Instant nanoTime1 = Instant.ofEpochSecond(0L, Clock.getDefault().now());
    Instant nanoTime2 = Instant.ofEpochSecond(0L, Clock.getDefault().now());
    assertTrue(instant.isBefore(nanoTime1));
    assertTrue(instant.isBefore(nanoTime2));
    assertTrue(nanoTime2.isAfter(nanoTime1));
  }

}
