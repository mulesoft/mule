/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingConditionNotMetException;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class CurrentSpanNameTracingConditionTestCase {

  public static final String EXPECTED_NAME = "expectedName";
  public static final String NON_EXPECTED_NAME = "nonExpectedName";

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void assertOnCurrentSpanOk() {
    CurrentSpanNameTracingCondition currentSpanNameTracingCondition = new CurrentSpanNameTracingCondition(EXPECTED_NAME);
    InternalSpan mockSpan = mock(InternalSpan.class);
    when(mockSpan.getName()).thenReturn(EXPECTED_NAME);
    currentSpanNameTracingCondition.assertOnCurrentSpan(mockSpan);
  }

  @Test
  public void assertOnCurrentSpanNull() {
    expectedException.expect(TracingConditionNotMetException.class);
    expectedException.expectMessage("The current span is null. Expected a span with name: " + EXPECTED_NAME);
    CurrentSpanNameTracingCondition currentSpanNameTracingCondition = new CurrentSpanNameTracingCondition(EXPECTED_NAME);
    currentSpanNameTracingCondition.assertOnCurrentSpan(null);
  }

  @Test
  public void assertOnCurrentSpanDifferentName() {
    expectedException.expect(TracingConditionNotMetException.class);
    expectedException.expectMessage("The current span has name: nonExpectedName.  Expected a span with name: " + EXPECTED_NAME);
    InternalSpan mockSpan = mock(InternalSpan.class);
    when(mockSpan.getName()).thenReturn(NON_EXPECTED_NAME);
    CurrentSpanNameTracingCondition currentSpanNameTracingCondition = new CurrentSpanNameTracingCondition(EXPECTED_NAME);
    currentSpanNameTracingCondition.assertOnCurrentSpan(mockSpan);
  }

}
