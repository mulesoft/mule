/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.impl.span.error.DefaultSpanError;

import java.util.function.Supplier;

/**
 * A {@link VoidCommand} that records an error
 *
 * The carrier is the {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextRecordErrorCommand extends AbstractFailsafeSpanVoidCommand {

  public static final String ERROR_MESSAGE = "Error recording a span error";

  private final EventContext eventContext;
  private final Supplier<Error> spanErrorSupplier;
  private final boolean isErrorEscapingCurrentSpan;
  private final FlowCallStack flowCallStack;

  public static VoidCommand getEventContextRecordErrorCommand(EventContext eventContext,
                                                              Supplier<Error> spanErrorSupplier,
                                                              boolean isErrorEscapingCurrentSpan,
                                                              FlowCallStack flowCallStack) {
    return new EventContextRecordErrorCommand(eventContext, spanErrorSupplier, isErrorEscapingCurrentSpan, flowCallStack);
  }

  public EventContextRecordErrorCommand(EventContext eventContext, Supplier<Error> spanErrorSupplier,
                                        boolean isErrorEscapingCurrentSpan, FlowCallStack flowCallStack) {
    this.eventContext = eventContext;
    this.spanErrorSupplier = spanErrorSupplier;
    this.isErrorEscapingCurrentSpan = isErrorEscapingCurrentSpan;
    this.flowCallStack = flowCallStack;
  }


  protected Runnable getRunnable() {
    return () -> {
      if (eventContext instanceof SpanContextAware) {
        ((SpanContextAware) eventContext)
            .getSpanContext()
            .recordErrorAtSpan(new DefaultSpanError(spanErrorSupplier.get(), flowCallStack,
                                                    isErrorEscapingCurrentSpan));
      }
    };
  }

  @Override
  protected String getErrorMessage() {
    return ERROR_MESSAGE;
  }
}
