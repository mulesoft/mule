/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.time;

import java.util.concurrent.TimeUnit;

/**
 * Represents a scalar amount of time expressed in a certain {@link TimeUnit}
 *
 * @since 4.0
 */
public final class Time {

  private final long time;
  private final TimeUnit timeUnit;

  /**
   * Creates a new instance
   *
   * @param time a scalar value representing a time
   * @param timeUnit a {@link TimeUnit} that qualifies the {@code time}
   */
  public Time(long time, TimeUnit timeUnit) {
    this.time = time;
    this.timeUnit = timeUnit;
  }

  /**
   * Returns a scalar time value
   *
   * @return a scalar time value as a native {@link long}
   */
  public long getTime() {
    return time;
  }

  /**
   * A {@link TimeUnit} which qualifies {@link #getTime()}
   *
   * @return a {@link TimeUnit}
   */
  public TimeUnit getUnit() {
    return timeUnit;
  }
}
