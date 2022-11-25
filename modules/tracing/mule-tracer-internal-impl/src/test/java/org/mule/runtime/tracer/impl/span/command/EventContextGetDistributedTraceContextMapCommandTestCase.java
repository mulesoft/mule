/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.EventContextGetDistributedTraceContextMapCommand.getEventContextGetDistributedTraceContextMapCommand;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.Matchers.anEmptyMap;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.span.InternalSpan;

import java.util.Map;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;


@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class EventContextGetDistributedTraceContextMapCommandTestCase {

  @Test
  public void getDistributedTraceContextMapIfSpanPresent() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));

    SpanContext spanContext = mock(SpanContext.class);
    when(eventContext.getSpanContext()).thenReturn(spanContext);

    InternalSpan span = mock(InternalSpan.class);
    when(spanContext.getSpan()).thenReturn(of(span));

    Map<String, String> serialization = mock(Map.class);
    when(span.serializeAsMap()).thenReturn(serialization);

    EventContextGetDistributedTraceContextMapCommand getDistributedTraceContextMapCommand =
        getEventContextGetDistributedTraceContextMapCommand((EventContext) eventContext);

    Map<String, String> map = getDistributedTraceContextMapCommand.execute();

    assertThat(map, equalTo(map));
  }

  @Test
  public void getDistributedTraceContextMapIfSpanNotPresent() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));

    SpanContext spanContext = mock(SpanContext.class);
    when(eventContext.getSpanContext()).thenReturn(spanContext);

    when(spanContext.getSpan()).thenReturn(empty());

    EventContextGetDistributedTraceContextMapCommand getDistributedTraceContextMapCommand =
        getEventContextGetDistributedTraceContextMapCommand((EventContext) eventContext);

    Map<String, String> map = getDistributedTraceContextMapCommand.execute();

    assertThat(map, anEmptyMap());
  }
}
