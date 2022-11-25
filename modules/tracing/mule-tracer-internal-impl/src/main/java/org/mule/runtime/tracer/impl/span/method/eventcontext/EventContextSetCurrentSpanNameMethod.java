/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.span.method.FailsafeDistributedTraceContextOperation;
import org.mule.runtime.tracer.impl.span.method.SetCurrentSpanNameMethod;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextGetter;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextSetCurrentSpanNameMethod;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class EventContextSetCurrentSpanNameMethod implements SetCurrentSpanNameMethod<EventContext> {

  private static final DistributedTraceContextGetter<EventContext> DISTRIBUTED_TRACE_CONTEXT_GETTER =
      EventContextDistributedTraceContextGetter.getDistributedTraceContextGetter();

  private static final SetCurrentSpanNameMethod<SpanContext> DISTRIBUTED_TRACE_CONTEXT_SET_CURRENT_SPAN_NAME_METHOD =
      DistributedTraceContextSetCurrentSpanNameMethod.getDistributedTraceContextSetCurrentSpanNameMethod();


  private static final Logger LOGGER = getLogger(EventContextSetCurrentSpanNameMethod.class);

  public static final String ERROR_MESSAGE = "Error when setting the current span name";

  private static final FailsafeDistributedTraceContextOperation FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION =
      FailsafeDistributedTraceContextOperation.getFailsafeDistributedTraceContextOperation(LOGGER,
                                                                                           ERROR_MESSAGE,
                                                                                           true);

  @Override
  public void setCurrentSpanName(EventContext context, String name) {
    FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION
        .execute(() -> DISTRIBUTED_TRACE_CONTEXT_SET_CURRENT_SPAN_NAME_METHOD
            .setCurrentSpanName(DISTRIBUTED_TRACE_CONTEXT_GETTER.getDistributedTraceContext(context), name));
  }
}
