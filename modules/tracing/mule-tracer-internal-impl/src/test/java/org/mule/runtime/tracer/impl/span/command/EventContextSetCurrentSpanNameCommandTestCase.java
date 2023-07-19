/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.EventContextSetCurrentSpanNameCommand.getEventContextSetCurrentSpanNameCommand;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.util.Optional.of;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.span.InternalSpan;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.slf4j.Logger;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class EventContextSetCurrentSpanNameCommandTestCase {

  public static final String EXPECTED_NAME = "expectedName";
  public static final String TEST_ERROR_MESSAGE = "Test error";

  @Test
  public void testSetCurrentSpanName() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));

    SpanContext spanContext = mock(SpanContext.class);
    when(eventContext.getSpanContext()).thenReturn(spanContext);

    InternalSpan span = mock(InternalSpan.class);
    when(spanContext.getSpan()).thenReturn(of(span));

    EventContextSetCurrentSpanNameCommand command =
        getEventContextSetCurrentSpanNameCommand(mock(Logger.class), TEST_ERROR_MESSAGE, true);

    command.execute((EventContext) eventContext, EXPECTED_NAME);

    verify(span).updateName(EXPECTED_NAME);
  }

}
