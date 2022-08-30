/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing;


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
public class SystemNanotimeClockTestCase {

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
