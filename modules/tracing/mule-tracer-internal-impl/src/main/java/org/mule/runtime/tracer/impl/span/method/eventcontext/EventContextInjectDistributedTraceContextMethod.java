/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.impl.span.method.FailsafeDistributedTraceContextOperation;
import org.mule.runtime.tracer.impl.span.method.InjectDistributedTraceContextMethod;
import org.mule.runtime.tracer.impl.context.EventSpanContext;

import org.slf4j.Logger;

public class EventContextInjectDistributedTraceContextMethod implements InjectDistributedTraceContextMethod<EventContext> {

  private static final org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextGetter<EventContext> DISTRIBUTED_TRACE_CONTEXT_GETTER =
      EventContextDistributedTraceContextGetter.getDistributedTraceContextGetter();


  private static final Logger LOGGER = getLogger(EventContextStartEventSpanMethod.class);

  public static final String ERROR_MESSAGE = "Error when starting a injecting distributed trace context to span";

  private static final FailsafeDistributedTraceContextOperation FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION =
      FailsafeDistributedTraceContextOperation.getFailsafeDistributedTraceContextOperation(LOGGER,
                                                                                           ERROR_MESSAGE, true);


  @Override
  public void inject(EventContext context, DistributedTraceContextGetter distributedTraceContextGetter) {
    FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION
        .execute(() -> doInject(context, distributedTraceContextGetter));
  }

  private void doInject(EventContext eventContext, DistributedTraceContextGetter distributedTraceContextGetter) {
    if (eventContext instanceof SpanContextAware) {
      ((SpanContextAware) eventContext).setSpanContext(
                                                       EventSpanContext.builder()
                                                           .withGetter(distributedTraceContextGetter)
                                                           .withPropagateTracingExceptions(
                                                                                           true)
                                                           .build());
    }
  }
}
