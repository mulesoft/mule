/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer.impl;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingCondition;
import org.mule.runtime.core.internal.profiling.tracing.event.tracer.TracingConditionNotMetException;

import java.util.Optional;

/**
 * A {@link TracingCondition} that indicates that the current span name must be the one indicated. If it is not, a
 * {@link TracingConditionNotMetException} will be raised.
 *
 * @since 4.5.0
 */
public class ComponentSpanNameTracingCondition implements TracingCondition {

  private final TracingCondition delegate;

  /**
   * @param expectedCurrentSpanName the expected current span name.
   */
  public ComponentSpanNameTracingCondition(String expectedCurrentSpanName) {
    if (expectedCurrentSpanName != null) {
      this.delegate = new CurrentSpanNameTracingCondition(expectedCurrentSpanName);
    } else {
      this.delegate = NO_CONDITION;
    }

  }

  @Override
  public void assertOnCurrentSpan(InternalSpan currentSpan) throws TracingConditionNotMetException {
    delegate.assertOnCurrentSpan(currentSpan);
  }
}
