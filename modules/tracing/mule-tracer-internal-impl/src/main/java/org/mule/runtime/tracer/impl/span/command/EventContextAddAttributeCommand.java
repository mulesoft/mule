/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import static org.mule.runtime.tracer.impl.span.method.eventcontext.SpanContextFromEventContextGetter.getSpanContextFromEventContextGetter;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.span.method.AbstractFailSafeSpanVoidCommand;
import org.mule.runtime.tracer.impl.span.method.VoidCommand;

/**
 * A {@link VoidCommand} that ads a span attribute.
 * The carrier is the {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextAddAttributeCommand extends AbstractFailSafeSpanVoidCommand {

  public static final String ERROR_MESSAGE = "Error adding a span attribute";

  private final EventContext eventContext;
  private final String key;
  private final String value;

  public static VoidCommand getEventContextAddSpanAttributeCommandFrom(EventContext eventContext, String key, String value) {
    return new EventContextAddAttributeCommand(eventContext, key, value);
  }

  private EventContextAddAttributeCommand(EventContext eventContext, String key, String value) {
    this.eventContext = eventContext;
    this.key = key;
    this.value = value;
  }

  @Override protected Runnable getRunnable() {
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
