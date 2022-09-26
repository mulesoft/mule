/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.tracer;

import static java.util.Collections.emptyMap;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.api.profiling.tracing.SpanError;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.event.trace.DistributedTraceContextGetter;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.privileged.profiling.tracing.SpanCustomizationInfo;

import java.util.Map;
import java.util.Optional;

/**
 * A tracer for {@link CoreEvent}'s.
 *
 * Once a core event is dispatched to a flow, it will hit different components. The processing of a core event in each component
 * will represent a {@link Span}, that is, a unit of work with a start time and an end time that encompasses what each component
 * does (a http request, a batch job, an until successful block, etc.). In each moment of a core event processing, the
 * {@link CoreEvent} will have a current context span, which is the most specific span associated to that core event, that is the
 * most specific processing that is being done in a certain moment.
 *
 * Example:
 *
 * A {@link CoreEvent} is dispatched to a flow and begins to perform a Http Request. The flow is being processed and also the
 * http:request, but the current context span is the span corresponding to the http request:
 *
 * ------------- Flow Span -----------------------------------------------------------> |________________ Http Request Span
 * ------- (Current Context Span) ------------>
 *
 * @since 4.5.0
 */
public interface CoreEventTracer {

  /**
   * Starts a span associated to the {@param component} as the current context span for the {@link CoreEvent}.
   *
   * @param coreEvent             the {@link CoreEvent} that has hit the {@link Component}
   * @param spanCustomizationInfo the {@link SpanCustomizationInfo} used for customizing the span.
   * @return the span generated for the context of the {@link CoreEvent} when it hits the {@param component} if it could be
   *         created.
   */
  Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent,
                                            SpanCustomizationInfo spanCustomizationInfo);


  /**
   * Starts a span associated to the {@param component} as the current context span for the {@link CoreEvent}.
   *
   * @param coreEvent             the {@link CoreEvent} that has hit the {@link Component}
   * @param spanCustomizationInfo the {@link SpanCustomizationInfo} used for customizing the span.
   * @param tracingCondition      indicates a condition that has to be verified for starting the span.
   * @return the span generated for the context of the {@link CoreEvent} when it hits the {@param component} if it could be
   *         created.
   */
  Optional<InternalSpan> startComponentSpan(CoreEvent coreEvent,
                                            SpanCustomizationInfo spanCustomizationInfo,
                                            TracingCondition tracingCondition);

  /**
   * @param coreEvent ends the current context {@link Span}.
   */
  void endCurrentSpan(CoreEvent coreEvent);

  /**
   * @param coreEvent ends the current context {@link Span}.
   * @param condition indicates a condition that has to be verified for ending the span.
   */
  void endCurrentSpan(CoreEvent coreEvent, TracingCondition condition);

  /**
   * Injects a distributedTraceContext in a {@link CoreEvent}
   *
   * @param eventContext                  the {@link EventContext} to inject.
   * @param distributedTraceContextGetter the {@link DistributedTraceContextGetter} to get the distributed trace context.
   */
  void injectDistributedTraceContext(EventContext eventContext, DistributedTraceContextGetter distributedTraceContextGetter);

  /**
   * Records a {@link SpanError} as part of the current {@link Span}.
   *
   * @param coreEvent                  The event to retrieve the distributed trace context from.
   * @param isErrorEscapingCurrentSpan True if the error is not being handled as part of the execution of the work that the
   *                                   {@link Span} containing the error represents.
   */
  void recordErrorAtCurrentSpan(CoreEvent coreEvent, boolean isErrorEscapingCurrentSpan);


  /**
   * @param event The event to retrieve the distributed trace context from.
   * @return a map containing the span context to propagate.
   */
  default Map<String, String> getDistributedTraceContextMap(CoreEvent event) {
    return emptyMap();
  }

  /**
   * Sets the current span name. It attempts to rename it if possible.
   *
   * @param coreEvent the {@link CoreEvent} to set the name to.
   * @param name      the name to set.
   */
  void setCurrentSpanName(CoreEvent coreEvent, String name);

  /**
   * Adds an attribute to the current span.
   *
   * @param coreEvent the coreEvent to add the attribute to.
   * @param key       the key for the span attribute
   * @param value     the value for the span attribute
   */
  void addCurrentSpanAttribute(CoreEvent coreEvent, String key, String value);

  /**
   * Adds the attribute to the current span.
   *
   * @param coreEvent  the coreEvent to add the attribute to.
   * @param attributes a map with the attributes to add.
   */
  void addCurrentSpanAttributes(CoreEvent coreEvent, Map<String, String> attributes);
}
