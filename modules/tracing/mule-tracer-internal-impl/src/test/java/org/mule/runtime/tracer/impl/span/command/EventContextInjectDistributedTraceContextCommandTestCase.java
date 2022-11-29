/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.EventContextInjectDistributedTraceContextCommand.getEventContextInjectDistributedTraceContextCommand;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.impl.context.EventSpanContext;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class EventContextInjectDistributedTraceContextCommandTestCase {

  @Test
  public void distributedTraceContextIsInjected() {
    SpanContextAware eventContext = mock(SpanContextAware.class, withSettings().extraInterfaces(EventContext.class));

    DistributedTraceContextGetter getter = mock(DistributedTraceContextGetter.class);

    VoidCommand injectDistributedTraceContextCommand =
        getEventContextInjectDistributedTraceContextCommand((EventContext) eventContext, getter);

    injectDistributedTraceContextCommand.execute();

    verify(eventContext).setSpanContext(any(EventSpanContext.class));

  }
}
