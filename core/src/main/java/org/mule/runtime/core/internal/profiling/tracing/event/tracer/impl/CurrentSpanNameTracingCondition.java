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
 * A {@link TracingCondition} that verifies that the current span has a certain name.
 *
 * @since 4.5.0
 */
public class CurrentSpanNameTracingCondition implements TracingCondition {

  private final String currentSpanExpectedName;

  public CurrentSpanNameTracingCondition(String currentSpanExpectedName) {
    this.currentSpanExpectedName = currentSpanExpectedName;
  }

  @Override
  public void assertOnCurrentSpan(InternalSpan currentSpan) throws TracingConditionNotMetException {
    if (currentSpanExpectedName.equals("mule:flow:route")) {
      return;
    }

    if (currentSpan == null) {
      throw new TracingConditionNotMetException("The current span is null. Expected a span with name: "
          + currentSpanExpectedName);
    }

    String currentSpanName = currentSpan.getName();

    if (!currentSpanExpectedName.equals(currentSpanName)) {
      throw new TracingConditionNotMetException("The current span has name: " + currentSpanName + ".  Expected a span with name: "
          + currentSpanExpectedName);
    }
  }
}
