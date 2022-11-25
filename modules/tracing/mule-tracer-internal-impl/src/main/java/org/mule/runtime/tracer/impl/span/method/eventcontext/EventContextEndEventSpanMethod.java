/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import static org.mule.runtime.tracer.api.span.validation.Assertion.SUCCESSFUL_ASSERTION;
import static org.mule.runtime.tracer.impl.span.method.eventcontext.EventContextDistributedTraceContextGetter.getDistributedTraceContextGetter;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.impl.span.method.EndEventSpanMethod;
import org.mule.runtime.tracer.impl.span.method.FailsafeDistributedTraceContextOperation;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextGetter;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextEndSpanMethod;
import org.slf4j.Logger;

/**
 * A {@link EndEventSpanMethod} that uses {@link EventContext} for the {@link SpanContext}.
 *
 * @since 4.5.0
 */
public class EventContextEndEventSpanMethod implements EndEventSpanMethod<EventContext> {

  private static final DistributedTraceContextGetter<EventContext> DISTRIBUTED_TRACE_CONTEXT_GETTER =
      getDistributedTraceContextGetter();

  private static final EndEventSpanMethod<SpanContext> DISTRIBUTED_TRACE_CONTEXT_END_METHOD =
      DistributedTraceContextEndSpanMethod.getDistributedTraceContextEndSpanMethod();

  private static final Logger LOGGER = getLogger(EventContextEndEventSpanMethod.class);

  public static final String ERROR_MESSAGE = "Error ending a component span";

  private static final FailsafeDistributedTraceContextOperation FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION =
      FailsafeDistributedTraceContextOperation.getFailsafeDistributedTraceContextOperation(LOGGER,
                                                                                           ERROR_MESSAGE,
                                                                                           true);

  @Override
  public void end(EventContext context) {
    end(context, SUCCESSFUL_ASSERTION);
  }

  @Override
  public void end(EventContext context, Assertion condition) {
    FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION
        .execute(() -> DISTRIBUTED_TRACE_CONTEXT_END_METHOD
            .end(DISTRIBUTED_TRACE_CONTEXT_GETTER.getDistributedTraceContext(context), condition));
  }
}
