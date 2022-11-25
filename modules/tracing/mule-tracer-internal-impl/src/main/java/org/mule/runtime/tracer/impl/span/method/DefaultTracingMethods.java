/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;
import org.mule.runtime.tracer.impl.span.method.eventcontext.EventContextAddSpanAttributeMethod;
import org.mule.runtime.tracer.impl.span.method.eventcontext.EventContextEndEventSpanMethod;
import org.mule.runtime.tracer.impl.span.method.eventcontext.EventContextGetTraceContextMapMethod;
import org.mule.runtime.tracer.impl.span.method.eventcontext.EventContextRecordSpanErrorMethod;
import org.mule.runtime.tracer.impl.span.method.eventcontext.EventContextSetCurrentSpanNameMethod;
import org.mule.runtime.tracer.impl.span.method.eventcontext.EventContextInjectDistributedTraceContextMethod;

import static org.mule.runtime.tracer.impl.span.method.eventcontext.EventContextStartEventSpanMethod.getEventContextStartEventSpanMethodBuilder;

/**
 * Default Tracing Commands
 *
 * @since 4.5.0
 */
public class DefaultTracingMethods {

  private DefaultTracingMethods() {}

  private static final EndEventSpanMethod<EventContext> END_EVENT_SPAN_METHOD = new EventContextEndEventSpanMethod();

  private static final RecordSpanErrorMethod<EventContext> RECORD_SPAN_ERROR_METHOD = new EventContextRecordSpanErrorMethod();

  private static final SetCurrentSpanNameMethod<EventContext> SET_CURRENT_SPAN_NAME_METHOD =
      new EventContextSetCurrentSpanNameMethod();

  private static final AddSpanAttributeMethod<EventContext> ADD_SPAN_ATTRIBUTES_METHOD = new EventContextAddSpanAttributeMethod();

  private static final InjectDistributedTraceContextMethod<EventContext> INJECT_DISTRIBUTED_TRACE_CONTEXT_METHOD =
      new EventContextInjectDistributedTraceContextMethod();

  private static final GetTraceContextMapMethod<EventContext> GET_DISTRIBUTED_CONTEXT_MAP_METHOD =
      new EventContextGetTraceContextMapMethod();

  public static StartEventSpanMethod<EventContext> getStartEventSpanMethod(MuleConfiguration muleConfiguration,
                                                                           ArtifactType artifactType,
                                                                           String artifactId,
                                                                           EventSpanFactory eventSpanFactory) {
    return getEventContextStartEventSpanMethodBuilder()
        .withMuleConfiguration(muleConfiguration)
        .withArtifactType(artifactType)
        .withArtifactId(artifactId)
        .withEventSpanFactory(eventSpanFactory)
        .build();
  }

  public static EndEventSpanMethod<EventContext> getEndSpanMethod() {
    return END_EVENT_SPAN_METHOD;
  }

  public static RecordSpanErrorMethod<EventContext> getRecordSpanErrorMethod() {
    return RECORD_SPAN_ERROR_METHOD;
  }

  public static SetCurrentSpanNameMethod<EventContext> getSetCurrentSpanNameMethod() {
    return SET_CURRENT_SPAN_NAME_METHOD;
  }

  public static AddSpanAttributeMethod<EventContext> getAddSpanAttributesMethod() {
    return ADD_SPAN_ATTRIBUTES_METHOD;
  }

  public static InjectDistributedTraceContextMethod<EventContext> getInjectDistributedTraceContextMethod() {
    return INJECT_DISTRIBUTED_TRACE_CONTEXT_METHOD;
  }

  public static GetTraceContextMapMethod<EventContext> getDistributedTraceContextMapMethod() {
    return GET_DISTRIBUTED_CONTEXT_MAP_METHOD;
  }
}
