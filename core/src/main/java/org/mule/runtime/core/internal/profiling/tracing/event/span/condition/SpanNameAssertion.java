/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.profiling.tracing.event.span.condition;

import org.mule.runtime.api.profiling.tracing.Span;
import org.mule.runtime.tracer.api.span.validation.Assertion;
import org.mule.runtime.tracer.api.span.validation.AssertionFailedException;

/**
 * A {@link Assertion} that verifies that the span has a certain name.
 *
 * @since 4.5.0
 */
public class SpanNameAssertion implements Assertion {

  private final String spanExpectedName;

  public SpanNameAssertion(String spanExpectedName) {
    this.spanExpectedName = spanExpectedName;
  }

  @Override
  public void assertOnSpan(Span span) throws AssertionFailedException {
    if (span == null) {
      throw new AssertionFailedException("The span is null. Expected a span with name: "
          + spanExpectedName);
    }

    String spanName = span.getName();

    if (!spanExpectedName.equals(spanName)) {
      throw new AssertionFailedException("The span has name: " + spanName + ".  Expected a span with name: "
          + spanExpectedName);
    }
  }
}
