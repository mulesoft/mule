/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.InternalSpan.getAsInternalSpan;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;

import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;

/**
 * An {@link AbstractFailsafeUnaryCommand} that gets the current distributed trace context map. The carrier is the
 * {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextGetDistributedTraceContextMapCommand extends
    AbstractFailsafeUnaryCommand<EventContext, Map<String, String>> {

  private final Function<EventContext, Map<String, String>> function;

  public static EventContextGetDistributedTraceContextMapCommand getEventContextGetDistributedTraceContextMapCommand(Logger logger,
                                                                                                                     String errorMessage,
                                                                                                                     boolean propagateException) {
    return new EventContextGetDistributedTraceContextMapCommand(logger, errorMessage, propagateException);
  }

  private EventContextGetDistributedTraceContextMapCommand(Logger logger, String errorMessage, boolean propagateException) {
    super(logger, errorMessage, propagateException, emptyMap());
    this.function = (eventContext) -> {
      if (eventContext instanceof SpanContextAware) {
        SpanContext spanContext =
            ((SpanContextAware) eventContext).getSpanContext();

        if (spanContext != null) {
          return spanContext.getSpan().map(span -> getAsInternalSpan(span).serializeAsMap()).orElse(emptyMap());
        }
      }
      return emptyMap();
    };
  }

  @Override
  Function<EventContext, Map<String, String>> getFunction() {
    return function;
  }
}
