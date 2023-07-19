/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.api.span.validation;

import org.mule.runtime.tracer.api.EventTracer;
import org.mule.runtime.tracer.api.span.InternalSpan;

/**
 * An assertion to make on a {@link InternalSpan}.
 *
 * @since 4.5.0
 */
public interface Assertion {

  Assertion SUCCESSFUL_ASSERTION = span -> {
  };

  /**
   * Assertion to perform on the span. If the span does not meet certain conditions an exception must be raised. This will be
   * invoked internally by the {@link EventTracer}.
   *
   * @param span the span to perform the assertion on.
   * @throws AssertionFailedException thrown if the assertion fails.
   */
  void assertOnSpan(InternalSpan span) throws AssertionFailedException;
}
