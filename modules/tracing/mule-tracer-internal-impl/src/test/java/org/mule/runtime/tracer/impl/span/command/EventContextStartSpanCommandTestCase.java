/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.EventContextStartSpanCommand.getEventContextStartSpanCommandFrom;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.Boolean.FALSE;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.EnrichedInitialSpanInfo;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;

import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.slf4j.Logger;


@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class EventContextStartSpanCommandTestCase {

  public static final String TEST_ERROR_MESSAGE = "Test error";

  @Test
  public void whenNotSpanContextAwareReturnEmpty() {
    EventContext eventContext = mock(EventContext.class);
    EventSpanFactory eventContextFactory = mock(EventSpanFactory.class);
    EnrichedInitialSpanInfo initialSpanInfo = mock(EnrichedInitialSpanInfo.class);
    Assertion assertion = mock(Assertion.class);

    EventContextStartSpanCommand startCommand = getEventContextStartSpanCommandFrom(mock(Logger.class),
                                                                                    TEST_ERROR_MESSAGE,
                                                                                    true,
                                                                                    eventContextFactory);

    Optional<InternalSpan> internalSpan = startCommand.execute(eventContext, initialSpanInfo, assertion);

    assertThat(internalSpan.isPresent(), equalTo(FALSE));
  }

  @Test
  public void whenSpanContextAwareReturnExpectedSpan() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));

    SpanContext spanContext = mock(SpanContext.class);
    when(eventContext.getSpanContext()).thenReturn(spanContext);

    EventSpanFactory eventContextFactory = mock(EventSpanFactory.class);

    EnrichedInitialSpanInfo initialSpanInfo = mock(EnrichedInitialSpanInfo.class);
    Assertion assertion = mock(Assertion.class);

    InternalSpan expectedSpan = mock(InternalSpan.class);
    when(eventContextFactory.getSpan(spanContext, initialSpanInfo)).thenReturn(expectedSpan);

    EventContextStartSpanCommand startCommand = getEventContextStartSpanCommandFrom(mock(Logger.class),
                                                                                    TEST_ERROR_MESSAGE,
                                                                                    true,
                                                                                    eventContextFactory);

    Optional<InternalSpan> internalSpan = startCommand.execute((EventContext) eventContext, initialSpanInfo, assertion);

    if (!internalSpan.isPresent()) {
      fail("No span present");
    }

    assertThat(internalSpan.get(), equalTo(expectedSpan));
  }
}
