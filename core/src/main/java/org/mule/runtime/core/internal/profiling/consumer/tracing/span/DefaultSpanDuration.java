/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span;

/**
 * Dafault implementation for {@link SpanDuration}
 */
public class DefaultSpanDuration implements SpanDuration {

  private final long startTimestamp;
  private long endTimestamp;

  public DefaultSpanDuration(long startTime) {
    this.startTimestamp = startTime;
  }

  @Override
  public long getStartEpochMillis() {
    return startTimestamp;
  }

  @Override
  public long getEndEpochMillis() {
    return endTimestamp;
  }

  @Override
  public long elapsedMillis() {
    if (endTimestamp == 0L) {
      return System.currentTimeMillis() - startTimestamp;
    } else {
      return endTimestamp - startTimestamp;
    }
  }

  @Override
  public void finish(long finishEpochMillis) {
    this.endTimestamp = finishEpochMillis;
  }
}
