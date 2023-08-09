/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.impl.span.InternalSpan;

import static java.util.Optional.of;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.slf4j.Logger;


@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class EventContextAddAttributeCommandTestCase {

  public static final String ATTRIBUTE_KEY = "ATTRIBUTE_KEY";
  public static final String ATTRIBUTE_VALUE = "ATTRIBUTE_VALUE";
  public static final String TEST_ERROR_MESSAGE = "Test error";

  @Test
  public void verifyAttributeIsAdded() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));

    SpanContext spanContext = mock(SpanContext.class);
    when(eventContext.getSpanContext()).thenReturn(spanContext);

    InternalSpan span = mock(InternalSpan.class);
    when(spanContext.getSpan()).thenReturn(of(span));

    EventContextAddAttributeCommand addAttributeCommand =
        EventContextAddAttributeCommand.getEventContextAddAttributeCommand(mock(Logger.class), TEST_ERROR_MESSAGE, true);

    addAttributeCommand.execute((EventContext) eventContext, ATTRIBUTE_KEY, ATTRIBUTE_VALUE);

    verify(span).addAttribute(ATTRIBUTE_KEY, ATTRIBUTE_VALUE);
  }
}
