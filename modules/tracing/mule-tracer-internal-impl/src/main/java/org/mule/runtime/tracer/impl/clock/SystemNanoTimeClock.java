/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.clock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A {@link Clock} that to obtain timestamp with nanotime precision.
 *
 * @since 4.5.0
 */
public class SystemNanoTimeClock implements Clock {

  private static final Clock INSTANCE = new SystemNanoTimeClock();
  private final long epochNanos;
  private final long nanoTime;

  public static Clock getInstance() {
    return INSTANCE;
  }

  private SystemNanoTimeClock() {
    this.epochNanos = MILLISECONDS.toNanos(System.currentTimeMillis());;
    this.nanoTime = System.nanoTime();
  }

  @Override
  public long now() {
    long deltaNanos = System.nanoTime() - this.nanoTime;
    return epochNanos + deltaNanos;
  }
}
