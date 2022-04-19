/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.consumer.tracing.span;

import org.mule.runtime.api.profiling.tracing.Span;

/**
 * A {@link Span} duration, delimited by epoch timestamps.
 *
 * @since 1.0.1
 */
public interface SpanDuration {

  /**
   * @return The timestamp where the duration starts, represented by the amount of milliseconds since the epoch.
   */
  long getStartEpochMillis();

  /**
   * @return 0 (zero) if the duration is still open or the timestamp where the duration ended, represented by the amount of milliseconds
   *         since the epoch.
   */
  long getEndEpochMillis();

  /**
   * @return The amount of milliseconds that comprises this {@link SpanDuration}
   */
  long elapsedMillis();

  /**
   * Closes this {@link SpanDuration} by providing a finish timestamp.
   * 
   * @param finishEpochMillis A finish timestamp, represented by the amount of milliseconds since the epoch.
   */
  void finish(long finishEpochMillis);

}
