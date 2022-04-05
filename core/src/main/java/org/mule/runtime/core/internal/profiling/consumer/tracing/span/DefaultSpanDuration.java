/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span;

import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.profiling.tracing.SpanDuration;

/**
 * Dafault implementation for {@link SpanDuration}
 */
public class DefaultSpanDuration implements SpanDuration {

  private final Long startTime;
  private final Long endTime;

  public DefaultSpanDuration(Long startTime, Long endTime) {
    requireNonNull(startTime);
    this.startTime = startTime;
    this.endTime = endTime;
  }

  @Override
  public Long getStart() {
    return startTime;
  }

  @Override
  public Long getEnd() {
    return endTime;
  }
}
