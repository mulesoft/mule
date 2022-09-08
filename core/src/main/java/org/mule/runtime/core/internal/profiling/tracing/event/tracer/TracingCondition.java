/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.profiling.tracing.event.tracer;

import org.mule.runtime.core.internal.profiling.tracing.event.span.InternalSpan;

/**
 * A condition that has to be met when a tracing operation is done.
 *
 * @since 4.5.0
 */
public interface TracingCondition {

  TracingCondition NO_CONDITION = new TracingCondition() {

    @Override
    public void assertOnCurrentSpan(InternalSpan currentSpan) throws TracingConditionNotMetException {

    }
  };

  /**
   * Assertion to perform on the current span. If the current span when the operation is performed does not meet certain
   * conditions an exception must be raised. This will be invoked internally by the {@link CoreEventTracer}.
   *
   * @param currentSpan the parentSpan.
   *
   * @throws TracingConditionNotMetException thrown if the condition was not met.
   */
  void assertOnCurrentSpan(InternalSpan currentSpan) throws TracingConditionNotMetException;
}
