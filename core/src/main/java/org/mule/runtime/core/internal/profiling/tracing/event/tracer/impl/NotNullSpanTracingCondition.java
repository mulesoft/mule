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

/**
 * A {@link TracingCondition} that fails if there is no current span set.
 *
 * @since 4.5.0
 */
public class NotNullSpanTracingCondition implements TracingCondition {

  private static final TracingCondition INSTANCE = new NotNullSpanTracingCondition();

  /**
   * @return an instance of {@link NotNullSpanTracingCondition}.
   */
  public static TracingCondition getNotNullSpanTracingCondition() {
    return INSTANCE;
  }

  private NotNullSpanTracingCondition() {}

  @Override
  public void assertOnSpan(InternalSpan span) throws TracingConditionNotMetException {
    if (span == null) {
      throw new TracingConditionNotMetException("No span set");
    }
  }
}
