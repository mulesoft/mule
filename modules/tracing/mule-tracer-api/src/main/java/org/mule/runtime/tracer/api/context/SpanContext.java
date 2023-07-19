/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.context;

import static java.util.Optional.empty;

import org.mule.runtime.tracer.api.span.SpanAware;
import org.mule.runtime.tracer.api.span.InternalSpan;
import org.mule.runtime.tracer.api.span.error.InternalSpanError;
import org.mule.runtime.tracer.api.span.validation.Assertion;

import java.util.Optional;

/**
 * Represents a context defined by a {@link InternalSpan}.
 *
 * @since 1.5.0
 */
public interface SpanContext extends SpanAware {

  static SpanContext emptySpanContext() {
    return new SpanContext() {

      @Override
      public void setSpan(InternalSpan span, Assertion assertion) {
        // Nothing to do.
      }

      @Override
      public Optional<InternalSpan> getSpan() {
        return empty();
      }

      @Override
      public SpanContext copy() {
        return this;
      }
    };
  }

  /**
   * @return a copy of the {@link SpanContext}
   */
  SpanContext copy();

  /**
   * @param assertion the {@link Assertion} to verify on end.
   */
  default void endSpan(Assertion assertion) {
    getSpan().ifPresent(InternalSpan::end);
  }

  /**
   * @param error the error to record.
   */
  default void recordErrorAtSpan(InternalSpanError error) {
    getSpan().ifPresent(currentSpan -> currentSpan.addError(error));
  }

}
