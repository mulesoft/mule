/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.api;

import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.tracer.api.sniffer.SpanSnifferManager;
import org.mule.runtime.tracer.api.context.getter.DistributedTraceContextGetter;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A tracer for {@link Event}'s.
 *
 * Once an event is dispatched to a flow, it will hit different components. The processing of an event in each component will
 * represent a {@link InternalSpan}, that is, a unit of work with a start time and an end time that encompasses what each
 * component does (a http request, a batch job, an until successful block, etc.). In each moment of an event processing, the
 * {@link Event} will have a context span, which is the most specific span associated to that event, that is the most specific
 * processing that is being done in a certain moment.
 *
 * Example:
 *
 * A {@link Event} is dispatched to a flow and begins to perform a Http Request. The flow is being processed and also the
 * http:request, but the current context span is the span corresponding to the http request:
 *
 * ------------- Flow Span -----------------------------------------------------------> |________________ Http Request Span
 * ------- (Current Context Span) ------------>
 *
 * @since 4.5.0
 */
public interface EventTracer<T extends Event> {

  // TODO: W-13057253: Refactor in order to provide Component related sugars.
  // TODO: Internal span should not be returned by this interface. Return Span instead. Improve static factories for getting the
  // internal span if necessary.
  /**
   * Starts a span associated to the {@param component} as the current context span for the {@link Event}.
   *
   * @param event    the {@link Event} that has hit the {@link Component}
   * @param spanInfo the {@link InitialSpanInfo} used for customizing the span.
   * @return the span generated for the context of the {@link Event} when it hits the {@param component} if it could be created.
   */
  Optional<InternalSpan> startComponentSpan(T event,
                                            InitialSpanInfo spanInfo);


  /**
   * Starts a span associated to the {@param component} as the current context span for the {@link Event}.
   *
   * @param event     the {@link Event} that has hit the {@link Component}
   * @param spanInfo  the {@link InitialSpanInfo} used for customizing the span.
   * @param assertion indicates a condition that has to be verified for starting the span.
   *
   * @return the span generated for the context of the {@link Event} when it hits the {@param component} if it could be created.
   */
  Optional<InternalSpan> startComponentSpan(T event,
                                            InitialSpanInfo spanInfo,
                                            Assertion assertion);

  /**
   * @param event ends the current context {@link InternalSpan}.
   */
  void endCurrentSpan(T event);

  /**
   * @param event     ends the current context {@link InternalSpan}.
   * @param condition indicates a condition that has to be verified for ending the span.
   */
  void endCurrentSpan(T event, Assertion condition);

  /**
   * Injects a distributedTraceContext in a {@link InternalSpan}
   *
   * @param eventContext                  the {@link EventContext} to inject.
   * @param distributedTraceContextGetter the {@link DistributedTraceContextGetter} to get the distributed trace context.
   */
  void injectDistributedTraceContext(EventContext eventContext, DistributedTraceContextGetter distributedTraceContextGetter);

  /**
   * Records an error as part of the current {@link InternalSpan}.
   *
   * @param event                      The event to retrieve the distributed trace context from. Must contain the
   *                                   {@link org.mule.runtime.api.message.Error} to be recorded.
   * @param isErrorEscapingCurrentSpan True if the error is not being handled as part of the execution of the work that the
   *                                   {@link InternalSpan} containing the error represents.
   */
  default void recordErrorAtCurrentSpan(T event, boolean isErrorEscapingCurrentSpan) {
    recordErrorAtCurrentSpan(event, () -> event.getError()
        .orElseThrow(() -> new IllegalArgumentException(String.format("Provided event [%s] does not declare an error.",
                                                                      event))),
                             isErrorEscapingCurrentSpan);
  }

  /**
   * Records an error as part of the current {@link InternalSpan}.
   *
   * @param event                      The event to retrieve the distributed trace context from.
   * @param errorSupplier              Supplier of the {@link org.mule.runtime.api.message.Error} that occurred.
   * @param isErrorEscapingCurrentSpan True if the error is not being handled as part of the execution of the work that the
   *                                   {@link InternalSpan} containing the error represents.
   */
  void recordErrorAtCurrentSpan(T event, Supplier<Error> errorSupplier, boolean isErrorEscapingCurrentSpan);


  /**
   * @param event The event to retrieve the distributed trace context from.
   * @return a map containing the span context to propagate.
   */
  default Map<String, String> getDistributedTraceContextMap(T event) {
    return emptyMap();
  }

  /**
   * Sets the current span name. It attempts to rename it if possible.
   *
   * @param event the {@link Event} to set the name to.
   * @param name  the name to set.
   */
  void setCurrentSpanName(T event, String name);

  /**
   * Adds an attribute to the current span.
   *
   * @param event the event to add the attribute to.
   * @param key   the key for the span attribute
   * @param value the value for the span attribute
   */
  void addCurrentSpanAttribute(T event, String key, String value);

  /**
   * Adds the attribute to the current span.
   *
   * @param event      the event to add the attribute to.
   * @param attributes a map with the attributes to add.
   */
  void addCurrentSpanAttributes(T event, Map<String, String> attributes);

  /**
   * @return a {@link SpanSnifferManager}.
   */
  SpanSnifferManager getSpanSnifferManager();
}
