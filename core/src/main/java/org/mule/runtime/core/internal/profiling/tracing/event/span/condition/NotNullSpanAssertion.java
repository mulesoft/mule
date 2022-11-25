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
  public void assertOnSpan(InternalSpan span) throws AssertionFailedException {
    if (span == null) {
      throw new AssertionFailedException("No span set");
    }
  }
}
