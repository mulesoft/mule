/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.impl.span.error.DefaultSpanError;

import java.util.function.Supplier;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.slf4j.Logger;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class EventContextRecordErrorCommandTestCase {

  public static final String TEST_ERROR_MESSAGE = "Test error";

  @Test
  public void recordError() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));
    CoreEvent coreEvent = mock(CoreEvent.class);
    when(coreEvent.getContext()).thenReturn((EventContext) eventContext);
    SpanContext spanContext = mock(SpanContext.class);
    when(eventContext.getSpanContext()).thenReturn(spanContext);

    Supplier<Error> spanErrorSupplier = () -> mock(Error.class);
    FlowCallStack flowStack = mock(FlowCallStack.class);
    EventContextRecordErrorCommand eventContextRecordErrorCommand =
        EventContextRecordErrorCommand.getEventContextRecordErrorCommand(mock(Logger.class),
                                                                         TEST_ERROR_MESSAGE, true);
    eventContextRecordErrorCommand.execute(coreEvent, spanErrorSupplier, true);

    verify(spanContext).recordErrorAtSpan(any(DefaultSpanError.class));
  }
}
