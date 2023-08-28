/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.InternalSpan.getAsInternalSpan;
import static org.mule.runtime.tracer.impl.span.command.spancontext.SpanContextFromEventContextGetter.getSpanContextFromEventContextGetter;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;

import java.util.function.BiConsumer;

import org.slf4j.Logger;

/**
 * An {@link AbstractFailSafeVoidBiCommand} that sets the current span name. The carrier is the {@link EventContext}
 *
 * @since 4.5.0
 */
public class EventContextSetCurrentSpanNameCommand extends AbstractFailSafeVoidBiCommand<EventContext, String> {

  private final BiConsumer<EventContext, String> consumer;

  public static EventContextSetCurrentSpanNameCommand getEventContextSetCurrentSpanNameCommand(Logger logger,
                                                                                               String errorMessage,
                                                                                               boolean propagateException) {
    return new EventContextSetCurrentSpanNameCommand(logger, errorMessage, propagateException);
  }

  private EventContextSetCurrentSpanNameCommand(Logger logger, String errorMessage, boolean propagateExceptions) {
    super(logger, errorMessage, propagateExceptions);
    this.consumer = (eventContext, name) -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      if (spanContext != null) {
        spanContext.getSpan().ifPresent(span -> getAsInternalSpan(span).updateName(name));
      }
    };
  }

  @Override
  BiConsumer<EventContext, String> getConsumer() {
    return consumer;
  }


}
