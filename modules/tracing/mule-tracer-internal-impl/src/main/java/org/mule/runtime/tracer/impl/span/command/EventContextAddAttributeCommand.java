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

import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

/**
 * An {@link AbstractFailSafeVoidTriCommand} that ads a span attribute. The carrier is the
 * {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextAddAttributeCommand extends AbstractFailSafeVoidTriCommand<EventContext, String, String> {

  private final TriFunction<EventContext, String, String, Void> triConsumer;

  public static EventContextAddAttributeCommand getEventContextAddAttributeCommand(Logger logger,
                                                                                   String errorMessage,
                                                                                   boolean propagateException) {
    return new EventContextAddAttributeCommand(logger, errorMessage, propagateException);
  }

  public EventContextAddAttributeCommand(Logger logger, String errorMessage, boolean propagateException) {
    super(logger, errorMessage, propagateException);
    this.triConsumer = (eventContext, key, value) -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      if (spanContext != null) {
        spanContext.getSpan().ifPresent(span -> span.addAttribute(key, value));
      }

      return null;
    };
  }


  @Override
  TriFunction<EventContext, String, String, Void> getTriConsumer() {
    return triConsumer;
  }

}
