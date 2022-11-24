/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.impl.span.method.AbstractFailSafeSpanVoidCommand;
import org.mule.runtime.tracer.impl.span.method.VoidCommand;

import static org.mule.runtime.tracer.impl.span.method.eventcontext.SpanContextFromEventContextGetter.getSpanContextFromEventContextGetter;

/**
 * A {@link VoidCommand} that ends the current {@link org.mule.runtime.tracer.api.span.InternalSpan}.
 * The carrier is the {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextEndSpanCommand extends AbstractFailSafeSpanVoidCommand {

  public static final String ERROR_MESSAGE = "Error ending a span";

  private final EventContext eventContext;
  private final Assertion assertion;

  private EventContextEndSpanCommand(EventContext eventContext, Assertion assertion) {
    this.eventContext = eventContext;
    this.assertion = assertion;
  }

  @Override protected Runnable getRunnable() {
    return () -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      if (spanContext != null) {
        spanContext.endSpan(assertion);
      }
    };
  }

  @Override protected String getErrorMessage() {
    return ERROR_MESSAGE;
  }
}
