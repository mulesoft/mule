/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.span.condition;

import org.mule.runtime.tracer.api.span.InternalSpan;
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
  public void assertOnSpan(InternalSpan span) throws AssertionFailedException {
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
