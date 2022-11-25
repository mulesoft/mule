/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import static org.mule.runtime.tracer.impl.span.method.eventcontext.EventContextDistributedTraceContextGetter.getDistributedTraceContextGetter;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.span.method.FailsafeDistributedTraceContextOperation;
import org.mule.runtime.tracer.impl.span.method.AddSpanAttributeMethod;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextGetter;
import org.mule.runtime.tracer.impl.span.method.distributedtracecontext.DistributedTraceContextAddSpanAttributeMethod;

import java.util.Map;

import org.slf4j.Logger;

/**
 * A {@link AddSpanAttributeMethod} that uses {@link EventContext} for the {@link SpanContext}.
 *
 * @since 4.5.0
 */
public class EventContextAddSpanAttributeMethod implements AddSpanAttributeMethod<EventContext> {

  private static final DistributedTraceContextGetter<EventContext> DISTRIBUTED_TRACE_CONTEXT_GETTER =
      getDistributedTraceContextGetter();

  private static final AddSpanAttributeMethod<SpanContext> DISTRIBUTED_TRACE_CONTEXT_ADD_ATTRIBUTE_METHOD =
      DistributedTraceContextAddSpanAttributeMethod.getDistributedTraceContextAddSpanAttributeMethod();

  private static final Logger LOGGER = getLogger(EventContextAddSpanAttributeMethod.class);

  public static final String ERROR_MESSAGE = "Error adding a span attribute";

  private static final FailsafeDistributedTraceContextOperation FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION =
      FailsafeDistributedTraceContextOperation.getFailsafeDistributedTraceContextOperation(LOGGER,
                                                                                           ERROR_MESSAGE,
                                                                                           true);

  @Override
  public void addAttribute(EventContext context, String key, String value) {
    FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION
        .execute(() -> DISTRIBUTED_TRACE_CONTEXT_ADD_ATTRIBUTE_METHOD
            .addAttribute(DISTRIBUTED_TRACE_CONTEXT_GETTER.getDistributedTraceContext(context), key, value));
  }

  @Override
  public void addAttributes(EventContext context, Map<String, String> attributes) {
    FAILSAFE_DISTRIBUTED_TRACE_CONTEXT_OPERATION
        .execute(() -> DISTRIBUTED_TRACE_CONTEXT_ADD_ATTRIBUTE_METHOD
            .addAttributes(DISTRIBUTED_TRACE_CONTEXT_GETTER.getDistributedTraceContext(context), attributes));
  }
}
