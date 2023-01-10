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
import org.mule.runtime.tracer.api.span.validation.Assertion;

import java.util.function.BiConsumer;

import org.slf4j.Logger;

/**
 * A {@link AbstractFailSafeVoidBiCommand} that ends the current {@link org.mule.runtime.tracer.api.span.InternalSpan}. The
 * carrier is the {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextEndSpanCommand extends AbstractFailSafeVoidBiCommand<EventContext, Assertion> {

  private final BiConsumer<EventContext, Assertion> consumer;

  public static EventContextEndSpanCommand getEventContextEndSpanCommandFrom(Logger logger,
                                                                             String errorMessage,
                                                                             boolean propagateException) {
    return new EventContextEndSpanCommand(logger, errorMessage, propagateException);
  }

  private EventContextEndSpanCommand(Logger logger, String errorMessage, boolean propagateExceptions) {
    super(logger, errorMessage, propagateExceptions);
    this.consumer = (eventContext, assertion) -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      if (spanContext != null) {
        spanContext.endSpan(assertion);
      }
    };
  }

  @Override
  BiConsumer<EventContext, Assertion> getConsumer() {
    return consumer;
  }
}
