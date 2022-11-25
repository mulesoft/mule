/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.context.notification.FlowCallStack;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.span.method.FailsafeDistributedTraceContextOperation;
import org.mule.runtime.tracer.impl.span.method.RecordSpanErrorMethod;
import org.mule.runtime.tracer.impl.span.error.DefaultSpanError;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextGetter;

import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * A {@link RecordSpanErrorMethod} that uses {@link EventContext} for the {@link SpanContext}.
 *
 * @since 4.5.0
 */
public class EventContextRecordSpanErrorMethod implements RecordSpanErrorMethod<EventContext> {

  private static final DistributedTraceContextGetter<EventContext> DISTRIBUTED_TRACE_CONTEXT_GETTER =
      EventContextDistributedTraceContextGetter.getDistributedTraceContextGetter();

  private static final Logger LOGGER = getLogger(EventContextRecordSpanErrorMethod.class);

  public static final String ERROR_MESSAGE = "Error when recording a component span error";

  private static final FailsafeDistributedTraceContextOperation FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION =
      FailsafeDistributedTraceContextOperation.getFailsafeDistributedTraceContextOperation(LOGGER,
                                                                                           ERROR_MESSAGE,
                                                                                           true);


  @Override
  public void recordError(EventContext context, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan,
                          FlowCallStack flowCallStack) {
    FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION
        .execute(() -> doRecordError(context, errorSupplier, isErrorEscapingCurrentSpan, flowCallStack));
  }

  private void doRecordError(EventContext eventContext, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan,
                             FlowCallStack flowCallStack) {
    if (eventContext instanceof SpanContextAware) {
      ((SpanContextAware) eventContext)
          .getSpanContext()
          .recordErrorAtSpan(new DefaultSpanError(errorSupplier.get(), flowCallStack,
                                                  isErrorEscapingCurrentSpan));
    }
  }
}
