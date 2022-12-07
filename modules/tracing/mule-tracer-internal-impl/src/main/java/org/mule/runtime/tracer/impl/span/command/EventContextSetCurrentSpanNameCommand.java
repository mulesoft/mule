/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.spancontext.SpanContextFromEventContextGetter.getSpanContextFromEventContextGetter;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;

/**
 * A {@link VoidCommand} that sets the current span name.
 *
 * The carrier is the {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextSetCurrentSpanNameCommand extends AbstractFailsafeSpanVoidCommand {

  public static final String ERROR_MESSAGE = "Error setting the current span name";
  private final EventContext eventContext;
  private final String name;

  public static VoidCommand getEventContextSetCurrentSpanNameCommand(EventContext eventContext,
                                                                     String name) {
    return new EventContextSetCurrentSpanNameCommand(eventContext, name);
  }

  public EventContextSetCurrentSpanNameCommand(EventContext eventContext, String name) {
    this.eventContext = eventContext;
    this.name = name;
  }

  protected Runnable getRunnable() {
    return () -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      if (spanContext != null) {
        spanContext.getSpan().ifPresent(span -> span.updateName(name));
      }
    };
  }

  @Override
  protected String getErrorMessage() {
    return ERROR_MESSAGE;
  }
}
