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
 * A {@link TracingCondition} that verifies that there is no current span set.
 *
 * @since 4.5.0
 */
public class NoMuleCurrentSpanSetTracingCondition implements TracingCondition {

  private static final TracingCondition INSTANCE = new NoMuleCurrentSpanSetTracingCondition();

  public static TracingCondition getNoMuleCurrentSpanSetTracingCondition() {
    return INSTANCE;
  }

  private NoMuleCurrentSpanSetTracingCondition() {}

  @Override
  public void assertOnCurrentSpan(InternalSpan currentSpan) throws TracingConditionNotMetException {
    if (currentSpan != null) {
      throw new TracingConditionNotMetException("Current span with name: " + currentSpan.getName()
          + " was found while no current span was expected.");
    }
  }
}
