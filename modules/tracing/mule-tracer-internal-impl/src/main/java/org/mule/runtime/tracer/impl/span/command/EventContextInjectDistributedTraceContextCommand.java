/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;

import static org.mule.runtime.tracer.impl.span.command.spancontext.SpanContextFromEventContextGetter.getSpanContextFromEventContextGetter;

/**
 * A {@link VoidCommand} that injects the span context.
 * 
 * The carrier is the {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextInjectSpanContextCommand extends AbstractFailsafeSpanVoidCommand {

  public static final String ERROR_MESSAGE = "Error injecting the span context";

  private final EventContext eventContext;
  private final String key;
  private final String value;

  public static VoidCommand getEventContextAddSpanAttributeCommandFrom(EventContext eventContext, String key, String value) {
    return new EventContextInjectSpanContextCommand(eventContext, key, value);
  }

  private EventContextInjectSpanContextCommand(EventContext eventContext, String key, String value) {
    this.eventContext = eventContext;
    this.key = key;
    this.value = value;
  }

  protected Runnable getRunnable() {
    return () -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      if (spanContext != null) {
        spanContext.getSpan().ifPresent(span -> span.addAttribute(key, value));
      }
    };
  }

  @Override protected String getErrorMessage() {
    return ERROR_MESSAGE;
  }
}
