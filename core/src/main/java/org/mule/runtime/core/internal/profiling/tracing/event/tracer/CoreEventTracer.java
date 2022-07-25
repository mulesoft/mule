/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.tracer;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.span.CoreEventSpanCustomizer;

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
   * @param coreEvent the {@link CoreEvent} that has hit the {@link Component}
   * @param component the {@link Component} that was hit by the {@link CoreEvent}
   *
   * @return the span generated for the context of the {@link CoreEvent} when it hits the {@param component}
   */
  InternalSpan startComponentSpan(CoreEvent coreEvent, Component component);

  /**
   * Starts a span associated to the {@param component} as the current context span for the {@link CoreEvent}.
   *
   * @param coreEvent               the {@link CoreEvent} that has hit the {@link Component}
   * @param component               the {@link Component} that was hit by the {@link CoreEvent}
   * @param coreEventSpanCustomizer the {@link CoreEventSpanCustomizer} used for customizing the span.
   *
   * @return the span generated for the context of the {@link CoreEvent} when it hits the {@param component}
   */
  InternalSpan startComponentSpan(CoreEvent coreEvent, Component component,
                                  CoreEventSpanCustomizer coreEventSpanCustomizer);

  /**
   * @param coreEvent ends the current context {@link Span}.
   */
  void endCurrentSpan(CoreEvent coreEvent);

}
