/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;

import java.util.Map;

import static org.mule.runtime.tracer.impl.span.command.spancontext.SpanContextFromEventContextGetter.getSpanContextFromEventContextGetter;

/**
 * A {@link VoidCommand} that ads span attributes. The carrier is the {@link EventContext}
 *
 * @since 4.5.0
 */
public class EventContextAddAttributesCommand extends AbstractFailsafeSpanVoidCommand {

  public static final String ERROR_MESSAGE = "Error adding a span attributes";

  private final EventContext eventContext;
  private final Map<String, String> attributes;

  public static VoidCommand getEventContextAddSpanAttributesCommandFrom(EventContext eventContext,
                                                                        Map<String, String> attributes) {
    return new EventContextAddAttributesCommand(eventContext, attributes);
  }

  private EventContextAddAttributesCommand(EventContext eventContext, Map<String, String> attributes) {
    this.eventContext = eventContext;
    this.attributes = attributes;
  }

  protected Runnable getRunnable() {
    return () -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      if (spanContext != null) {
        spanContext.getSpan().ifPresent(span -> attributes.forEach(span::addAttribute));
      }
    };
  }

  @Override
  protected String getErrorMessage() {
    return ERROR_MESSAGE;
  }
}
