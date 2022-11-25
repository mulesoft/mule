/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method.eventcontext;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.tracer.api.context.SpanContextAware;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.impl.DefaultCoreEventTracerUtils;
import org.mule.runtime.tracer.impl.span.method.GetTraceContextMapMethod;
import org.slf4j.Logger;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.slf4j.LoggerFactory.getLogger;

public class EventContextGetTraceContextMapMethod implements GetTraceContextMapMethod<EventContext> {

  private static final Logger LOGGER = getLogger(EventContextGetTraceContextMapMethod.class);

  public static final String ERROR_MESSAGE = "Error when starting a injecting distributed trace context to span";


  @Override
  public Map<String, String> getDistributedTraceContextMap(EventContext context) {
    return DefaultCoreEventTracerUtils.safeExecuteWithDefaultOnThrowable(() -> doGetDistributedTraceContextMap(context),
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
