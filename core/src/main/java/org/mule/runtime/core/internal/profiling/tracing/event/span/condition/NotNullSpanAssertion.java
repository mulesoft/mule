/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span.condition;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.validation.AssertionFailedException;

/**
 * A {@link Assertion} that fails if there is no span set.
 *
 * @since 4.5.0
 */
public class NotNullSpanAssertion implements Assertion {

  private static final Assertion INSTANCE = new NotNullSpanAssertion();

  /**
   * @return an instance of {@link NotNullSpanAssertion}.
   */
  public static Assertion getNotNullSpanTracingCondition() {
    return INSTANCE;
  }

  private NotNullSpanAssertion() {}

  @Override
  public void assertOnSpan(Span span) throws AssertionFailedException {
    if (span == null) {
      throw new AssertionFailedException("No span set");
    }
  }
}
