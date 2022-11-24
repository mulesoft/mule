/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.SafeExecutionUtils.safeExecuteWithDefaultOnThrowable;

import static java.util.Collections.emptyMap;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.slf4j.Logger;

import java.util.Map;

/**
 * A {@link Command} gets the current distributed trace context map.
 *
 * The carrier is the {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextGetDistributedTraceContextMapCommand implements Command<Map<String, String>> {

  private static final Logger LOGGER = getLogger(EventContextGetDistributedTraceContextMapCommand.class);

  public static final String ERROR_MESSAGE = "Error when starting a injecting distributed trace context to span";

  private final EventContext eventContext;

  public static EventContextGetDistributedTraceContextMapCommand getEventContextGetDistributedTraceContextMapCommand(EventContext eventContext) {
    return new EventContextGetDistributedTraceContextMapCommand(eventContext);
  }

  private EventContextGetDistributedTraceContextMapCommand(EventContext eventContext) {
    this.eventContext = eventContext;
  }

  @Override
  public Map<String, String> execute() {
    return safeExecuteWithDefaultOnThrowable(() -> doGetDistributedTraceContextMap(eventContext),
                                             emptyMap(),
                                             "Error on getting distrinuted trace context map",
                                             true,
                                             LOGGER);
  }

  private Map<String, String> doGetDistributedTraceContextMap(EventContext eventContext) {
    if (eventContext instanceof SpanContextAware) {
      SpanContext distributedTraceContext =
          ((SpanContextAware) eventContext).getSpanContext();

      return distributedTraceContext.getSpan().map(InternalSpan::serializeAsMap).orElse(emptyMap());

    } else {
      return emptyMap();
    }
  }
}
