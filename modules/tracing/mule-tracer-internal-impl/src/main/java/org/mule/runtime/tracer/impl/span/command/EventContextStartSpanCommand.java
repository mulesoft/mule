/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.tracer.impl.span.command.SpanMDCUtils.setCurrentTracingInformationToMdc;
import static org.mule.runtime.tracer.impl.span.command.spancontext.SpanContextFromEventContextGetter.getSpanContextFromEventContextGetter;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.impl.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.InitialSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;
import org.slf4j.Logger;

import java.util.Optional;

import org.apache.commons.lang3.function.TriFunction;

/**
 * An {@link AbstractFailsafeTriCommand} that starts the current {@link InternalSpan}. The carrier is the
 * {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextStartSpanCommand extends
    AbstractFailsafeTriCommand<Optional<Span>, EventContext, InitialSpanInfo, Assertion> {

  private final TriFunction<EventContext, InitialSpanInfo, Assertion, Optional<Span>> triFunction;

  public static EventContextStartSpanCommand getEventContextStartSpanCommandFrom(Logger logger,
                                                                                 String errorMessage,
                                                                                 boolean propagateException,
                                                                                 EventSpanFactory eventSpanFactory,
                                                                                 boolean spanIdAndTraceIdInMdc) {
    return new EventContextStartSpanCommand(logger, errorMessage, propagateException, eventSpanFactory, spanIdAndTraceIdInMdc);
  }

  private EventContextStartSpanCommand(Logger logger, String errorMessage, boolean propagateException,
                                       EventSpanFactory eventSpanFactory,
                                       boolean spanIdAndTraceIdInMdc) {
    super(logger, errorMessage, propagateException, empty());
    this.triFunction = (eventContext, initialSpanInfo, assertion) -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      InternalSpan newSpan = null;

      if (spanContext != null) {
        newSpan = eventSpanFactory.getSpan(spanContext, initialSpanInfo);
        spanContext.setSpan(newSpan, assertion);
      }

      if (spanIdAndTraceIdInMdc && newSpan != null) {
        setCurrentTracingInformationToMdc(newSpan);
      }

      return ofNullable(newSpan);
    };
  }

  @Override
  TriFunction<EventContext, InitialSpanInfo, Assertion, Optional<Span>> getTriFunction() {
    return triFunction;
  }

}
