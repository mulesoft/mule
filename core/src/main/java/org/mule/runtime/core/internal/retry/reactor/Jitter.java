/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * Jitter function that is applied to the backoff delay.
 *
 */
public interface Jitter extends Function<BackoffDelay, Duration> {

  /**
   * Jitter function that is a no-op.
   * @return Jitter function that does not apply any jitter
   */
  static Jitter noJitter() {
    return backoff -> backoff.delay();
  }

  /**
   * Jitter function that applies a random jitter to choose a random backoff
   * delay between {@link BackoffDelay#minDelay()} and {@link BackoffDelay#delay()}.
   * @return Jitter function to randomize backoff delay
   */
  static Jitter random() {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    return backoff -> {
      long backoffMs = backoff.delay().toMillis();
      long minBackoffMs = backoff.min.toMillis();
      long jitterBackoffMs = backoffMs == minBackoffMs ? minBackoffMs : random.nextLong(minBackoffMs, backoffMs);
      return Duration.ofMillis(jitterBackoffMs);
    };
  }
}
