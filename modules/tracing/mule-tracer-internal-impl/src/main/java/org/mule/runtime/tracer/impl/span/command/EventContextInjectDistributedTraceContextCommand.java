/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.context.EventSpanContext.builder;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;

/**
 * A {@link VoidCommand} that injects the span context.
 *
 * The carrier is the {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextInjectDistributedTraceContextCommand extends AbstractFailsafeSpanVoidCommand {

  public static final String ERROR_MESSAGE = "Error injecting the span context";

  private final EventContext eventContext;
  private final DistributedTraceContextGetter getter;

  public static VoidCommand getEventContextInjectDistributedTraceContextCommand(EventContext eventContext,
                                                                                DistributedTraceContextGetter getter) {
    return new EventContextInjectDistributedTraceContextCommand(eventContext, getter);
  }

  private EventContextInjectDistributedTraceContextCommand(EventContext eventContext, DistributedTraceContextGetter getter) {
    this.eventContext = eventContext;
    this.getter = getter;
  }

  protected Runnable getRunnable() {
    return () -> {
      if (eventContext instanceof SpanContextAware) {
        ((SpanContextAware) eventContext).setSpanContext(
                                                         builder()
                                                             .withGetter(getter)
                                                             .build());
      }
    };
  }

  @Override
  protected String getErrorMessage() {
    return ERROR_MESSAGE;
  }
}
