/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import static org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl.NotNullSpanTracingCondition.getNotNullSpanTracingCondition;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;

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
public class NotNullSpanTracingConditionTestCase {

  @Rule
  public ExpectedException expectedException = none();


  @Test
  public void assertOk() {
    TracingCondition condition = getNotNullSpanTracingCondition();
    condition.assertOnSpan(mock(InternalSpan.class));
  }

  @Test
  public void assertFail() {
    expectedException.expect(TracingConditionNotMetException.class);
    expectedException.expectMessage("No span set");
    TracingCondition condition = getNotNullSpanTracingCondition();
    condition.assertOnSpan(null);
  }

}
