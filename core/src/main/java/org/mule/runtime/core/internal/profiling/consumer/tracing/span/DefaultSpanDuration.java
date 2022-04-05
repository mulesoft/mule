/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span;

import org.mule.runtime.api.profiling.tracing.SpanDuration;

/**
 * Dafault implementation for {@link SpanDuration}
 */
public class DefaultSpanDuration implements SpanDuration {

  private final long startTime;
  private final long endTime;

  public DefaultSpanDuration(long startTime, long endTime) {
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @Override
  public long getStart() {
    return startTime;
  }

  @Override
  public long getEnd() {
    return endTime;
  }
}
