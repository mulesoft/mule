/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.spancontext.SpanContextFromEventContextGetter.getSpanContextFromEventContextGetter;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.slf4j.Logger;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * A {@link AbstractFailSafeVoidBiCommand} that adds span attributes. The carrier is the {@link EventContext}
 *
 * @since 4.5.0
 */
public class EventContextAddAttributesCommand extends AbstractFailSafeVoidBiCommand<EventContext, Map<String, String>> {

  private BiConsumer<EventContext, Map<String, String>> consumer;

  public static EventContextAddAttributesCommand getEventContextAddAttributesCommand(Logger logger,
                                                                                     String errorMessage,
                                                                                     boolean propagateException) {
    return new EventContextAddAttributesCommand(logger, errorMessage, propagateException);
  }

  private EventContextAddAttributesCommand(Logger logger, String errorMessage, boolean propagateExceptions) {
    super(logger, errorMessage, propagateExceptions);
    this.consumer = (eventContext, attributes) -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      if (spanContext != null) {
        spanContext.getSpan().ifPresent(span -> attributes.forEach(span::addAttribute));
      }
    };
  }

  @Override
  BiConsumer<EventContext, Map<String, String>> getConsumer() {
    return consumer;
  }
}
