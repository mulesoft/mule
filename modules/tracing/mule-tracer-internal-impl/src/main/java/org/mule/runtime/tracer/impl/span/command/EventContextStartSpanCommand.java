/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.tracer.api.context.SpanContext;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.info.StartSpanInfo;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.impl.span.factory.EventSpanFactory;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;
import static org.mule.runtime.tracer.impl.span.command.spancontext.SpanContextFromEventContextGetter.getSpanContextFromEventContextGetter;

/**
 * A {@link VoidCommand} that starts the current {@link org.mule.runtime.tracer.api.span.InternalSpan}. The carrier is the
 * {@link org.mule.runtime.api.event.EventContext}
 *
 * @since 4.5.0
 */
public class EventContextStartSpanCommand extends AbstractFailsafeSpanInternalSpanCommand {

  public static final String ERROR_MESSAGE = "Error starting a span";

  private final EventContext eventContext;
  private final Assertion assertion;
  private final EventSpanFactory eventSpanFactory;
  private final StartSpanInfo startSpaninfo;

  /**
   *
   * @param eventContext     the {@link EventContext}.xs
   * @param eventSpanFactory the {@link EventSpanFactory} to create the span.
   * @param startSpanInfo    the {@link StartSpanInfo} to indicate how the {@link InternalSpan} should be created.
   * @param assertion        the {@link Assertion} to validate when starting the {@link EventContext}.
   * @return
   */
  public static EventContextStartSpanCommand getEventContextStartSpanCommandFrom(EventContext eventContext,
                                                                                 EventSpanFactory eventSpanFactory,
                                                                                 StartSpanInfo startSpanInfo,
                                                                                 Assertion assertion) {
    return new EventContextStartSpanCommand(eventContext, eventSpanFactory, startSpanInfo, assertion);
  }

  private EventContextStartSpanCommand(EventContext eventContext,
                                       EventSpanFactory eventSpanFactory,
                                       StartSpanInfo startSpanInfo,
                                       Assertion assertion) {
    this.eventSpanFactory = eventSpanFactory;
    this.eventContext = eventContext;
    this.assertion = assertion;
    this.startSpaninfo = startSpanInfo;
  }

  protected Supplier<Optional<InternalSpan>> getSupplier() {
    return () -> {
      SpanContext spanContext = getSpanContextFromEventContextGetter().get(eventContext);

      InternalSpan newSpan = null;

      if (spanContext != null) {
        newSpan = eventSpanFactory.getSpan(spanContext, startSpaninfo);
        spanContext.setSpan(newSpan, assertion);
      }

      return ofNullable(newSpan);
    };
  }

  @Override
  protected String getErrorMessage() {
    return ERROR_MESSAGE;
  }
}
