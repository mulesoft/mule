/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.EventContextEndSpanCommand.getEventContextEndSpanCommandFrom;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.span.validation.Assertion;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.slf4j.Logger;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class EventContextEndSpanCommandTestCase {

  public static final String TEST_ERROR = "Test error";

  @Test
  public void verifyEndsSpanIfPresent() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));

    SpanContext spanContext = mock(SpanContext.class);
    when(eventContext.getSpanContext()).thenReturn(spanContext);

    Assertion assertion = mock(Assertion.class);

    EventContextEndSpanCommand endCommand =
        getEventContextEndSpanCommandFrom(mock(Logger.class), TEST_ERROR, true, true);
    endCommand.execute((EventContext) eventContext, assertion);

    verify(spanContext).endSpan(assertion);
  }
}
