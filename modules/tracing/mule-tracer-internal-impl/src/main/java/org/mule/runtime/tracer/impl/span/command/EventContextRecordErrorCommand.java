/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.impl.span.error.DefaultSpanCallStack;
import org.mule.runtime.tracer.impl.span.error.DefaultSpanError;

import java.util.function.Supplier;

import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

/**
 * An {@link AbstractFailSafeVoidTriCommand} that records an error
 *
 * The carrier is the {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextRecordErrorCommand extends AbstractFailSafeVoidTriCommand<CoreEvent, Supplier<Error>, Boolean> {

  private final TriFunction<CoreEvent, Supplier<Error>, Boolean, Void> triConsumer;

  public static EventContextRecordErrorCommand getEventContextRecordErrorCommand(Logger logger,
                                                                                 String errorMessage,
                                                                                 boolean propagateException) {
    return new EventContextRecordErrorCommand(logger, errorMessage, propagateException);
  }

  public EventContextRecordErrorCommand(Logger logger, String errorMessage, boolean propagateException) {
    super(logger, errorMessage, propagateException);
    this.triConsumer = (coreEvent, spanErrorSupplier, isErrorEscapingCurrentSpan) -> {
      EventContext eventContext = coreEvent.getContext();
      if (eventContext instanceof SpanContextAware) {
        ((SpanContextAware) eventContext)
            .getSpanContext()
            .recordErrorAtSpan(new DefaultSpanError(spanErrorSupplier.get(),
                                                    new DefaultSpanCallStack(coreEvent.getFlowCallStack()),
                                                    isErrorEscapingCurrentSpan));
      }

      return null;
    };
  }


  @Override
  TriFunction<CoreEvent, Supplier<Error>, Boolean, Void> getTriConsumer() {
    return triConsumer;
  }
}
