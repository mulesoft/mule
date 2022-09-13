/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.NullSpanTracingCondition.getNullSpanTracingCondition;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingConditionNotMetException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class NullSpanTracingConditionTestCase {

  public static final String EXPECTED_SPAN_NAME = "expectedSpanName";
  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void assertOk() {
    TracingCondition condition = getNullSpanTracingCondition();
    condition.assertOnSpan(null);
  }

  @Test
  public void assertFail() {
    expectedException.expect(TracingConditionNotMetException.class);
    expectedException.expectMessage("Span with name: expectedSpanName was found while no span was expected.");
    TracingCondition condition = getNullSpanTracingCondition();
    InternalSpan span = mock(InternalSpan.class);
    when(span.getName()).thenReturn(EXPECTED_SPAN_NAME);
    condition.assertOnSpan(span);
  }

}
