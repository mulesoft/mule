/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tracer.api.component;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.tracer.api.span.validation.Assertion;

import java.util.Optional;

/**
 * {@link ComponentTracer} implementations will support the tracing of different {@link Component}s.
 *
 * @param <T> The {@link Event} that will be used as a carrier of a {@link org.mule.runtime.tracer.api.context.SpanContext}
 *
 * @see org.mule.runtime.tracer.api.EventTracer
 * @since 4.5.0
 */
public interface ComponentTracer<T extends Event> {

  /**
   * Starts a {@link Span} associated to the {@link Component}.
   *
   * @param event the {@link Event} that has hit the {@link Component}.
   * @return the {@link Span} generated for the context of the {@link Event} when it hits the {@link Component} if it could be
   *         created.
   */
  Optional<Span> startSpan(T event);

  /**
   * Starts a {@link Span} associated to the {@link Component}.
   *
   * @param event the {@link Event} that has hit the {@link Component}.
   */
  void endCurrentSpan(T event);

  /**
   * Adds an attibute to the current {@link Span} associated to the {@link Component}.
   *
   * @param event the {@link Event} that has hit the {@link Component}.
   */
  void addCurrentSpanAttribute(T event, String key, String value);

  /**
   *
   * @return The assertion used to validate that a span is associated to the {@link Component} that this tracer instruments.
   */
  Assertion getComponentSpanAssertion();
}
