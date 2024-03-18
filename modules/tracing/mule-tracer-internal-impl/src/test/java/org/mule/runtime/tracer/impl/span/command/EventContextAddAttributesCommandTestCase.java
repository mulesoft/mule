/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.EventContextAddAttributesCommand.getEventContextAddAttributesCommand;
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
import org.mule.runtime.tracer.impl.span.InternalSpan;

import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.slf4j.Logger;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class EventContextAddAttributesCommandTestCase {

  public static final String ATTRIBUTE_KEY_1 = "ATTRIBUTE_KEY_1";
  public static final String ATTRIBUTE_VALUE_1 = "ATTRIBUTE_VALUE_1";
  public static final String ATTRIBUTE_KEY_2 = "ATTRIBUTE_KEY_2";
  public static final String ATTRIBUTE_VALUE_2 = "ATTRIBUTE_VALUE_2";
  public static final String TEST_ERROR_MESSAGE = "Test error";

  @Test
  public void verifyAttributesAreAdded() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));

    SpanContext spanContext = mock(SpanContext.class);
    when(eventContext.getSpanContext()).thenReturn(spanContext);

    InternalSpan span = mock(InternalSpan.class);
    when(spanContext.getSpan()).thenReturn(of(span));

    Map<String, String> attributes = new HashMap<>();
    attributes.put(ATTRIBUTE_KEY_1, ATTRIBUTE_VALUE_1);
    attributes.put(ATTRIBUTE_KEY_2, ATTRIBUTE_VALUE_2);

    EventContextAddAttributesCommand addAttributeCommand =
        getEventContextAddAttributesCommand(mock(Logger.class), TEST_ERROR_MESSAGE, true);

    addAttributeCommand.execute((EventContext) eventContext, attributes);

    verify(span).addAttribute(ATTRIBUTE_KEY_1, ATTRIBUTE_VALUE_1);
    verify(span).addAttribute(ATTRIBUTE_KEY_2, ATTRIBUTE_VALUE_2);
  }
}
