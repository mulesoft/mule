/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.span;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.tracer.api.span.validation.Assertion;

import java.util.Optional;

/**
 * A component that has a {@link org.mule.runtime.api.profiling.tracing.Span}.
 *
 * @since 4.5.0
 */
public interface SpanAware {

  /**
   * @param span      set the {@link Span}
   * @param assertion the tracing condition to assert on setting the span
   */
  void setSpan(Span span, Assertion assertion);

  /**
   * @return the owned {@link Span}
   */
  Optional<Span> getSpan();
}
