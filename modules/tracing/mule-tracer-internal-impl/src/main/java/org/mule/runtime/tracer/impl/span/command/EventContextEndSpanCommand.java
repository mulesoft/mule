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
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.validation.Assertion;

import java.util.Optional;

/**
 * A {@link VoidCommand} that ends the current {@link org.mule.runtime.tracer.api.span.InternalSpan}. The carrier is the
 * {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextEndSpanCommand extends AbstractFailsafeSpanVoidCommand {

  public static final String ERROR_MESSAGE = "Error ending a span";
  public static final String THREAD_END_SPAN = "thread.end.name";

  private final EventContext eventContext;
  private final Assertion assertion;

  /**
   * Return a {@link VoidCommand} that ends the current span in the {@link EventContext}
   *
   * @param eventContext the {@link EventContext}.
   * @param assertion    the {@link Assertion} to validate when ending the {@link EventContext}.
   *
   * @return the {@link EventContextEndSpanCommand}.
   */
  public static EventContextEndSpanCommand getEventContextEndSpanCommandFrom(EventContext eventContext, Assertion assertion) {
    return new EventContextEndSpanCommand(eventContext, assertion);
  }

  private EventContextEndSpanCommand(EventContext eventContext, Assertion assertion) {
    this.eventContext = eventContext;
    this.assertion = assertion;
  }

  protected Runnable getRunnable() {
    return () -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      if (spanContext != null) {
        Optional<InternalSpan> span = spanContext.getSpan();
        spanContext.endSpan(assertion);
      }
    };
  }

  @Override
  protected String getErrorMessage() {
    return ERROR_MESSAGE;
  }
}
