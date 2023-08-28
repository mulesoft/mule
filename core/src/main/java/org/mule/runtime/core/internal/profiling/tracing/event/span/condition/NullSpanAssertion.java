/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span.condition;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.validation.AssertionFailedException;

/**
 * A {@link Assertion} that verifies that there is no span set.
 *
 * @since 4.5.0
 */
public class NullSpanAssertion implements Assertion {

  private static final Assertion INSTANCE = new NullSpanAssertion();

  public static Assertion getNullSpanTracingCondition() {
    return INSTANCE;
  }

  private NullSpanAssertion() {}

  @Override
  public void assertOnSpan(Span span) throws AssertionFailedException {
    if (span != null) {
      throw new AssertionFailedException("Span with name: " + span.getName()
          + " was found while no span was expected.");
    }
  }
}
