/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

import java.time.Duration;
import java.util.function.Function;

/**
 * Backoff function
 *
 */
public interface Backoff extends Function<Context<?>, BackoffDelay> {

  public static final Backoff ZERO_BACKOFF = context -> BackoffDelay.ZERO;

  /**
   * Backoff function with no backoff delay
   * @return Backoff function for zero backoff delay
   */
  static Backoff zero() {
    return ZERO_BACKOFF;
  }

  /**
   * Backoff function with fixed backoff delay
   * @param backoffInterval backoff interval
   * @return Backoff function with fixed backoff delay
   */
  static Backoff fixed(Duration backoffInterval) {
    return context -> new BackoffDelay(backoffInterval);
  }

  /**
   * Backoff function with exponential backoff delay. Retries are performed after a backoff
   * interval of <code>firstBackoff * (factor ** n)</code> where n is the iteration. If
   * <code>maxBackoff</code> is not null, the maximum backoff applied will be limited to
   * <code>maxBackoff</code>.
   * <p>
   * If <code>basedOnPreviousValue</code> is true, backoff will be calculated using
   * <code>prevBackoff * factor</code>. When backoffs are combined with {@link Jitter}, this
   * value will be different from the actual exponential value for the iteration.
   *
   * @param firstBackoff First backoff duration
   * @param maxBackoff Maximum backoff duration
   * @param factor The multiplicand for calculating backoff
   * @param basedOnPreviousValue If true, calculation is based on previous value which may
   *        be a backoff with jitter applied
   * @return Backoff function with exponential delay
   */
  static Backoff exponential(Duration firstBackoff, Duration maxBackoff, int factor, boolean basedOnPreviousValue) {
    if (firstBackoff == null || firstBackoff.isNegative() || firstBackoff.isZero())
      throw new IllegalArgumentException("firstBackoff must be > 0");
    Duration maxBackoffInterval = maxBackoff != null ? maxBackoff : Duration.ofSeconds(Long.MAX_VALUE);
    if (maxBackoffInterval.compareTo(firstBackoff) <= 0)
      throw new IllegalArgumentException("maxBackoff must be >= firstBackoff");
    if (!basedOnPreviousValue) {
      return context -> {
        Duration nextBackoff = firstBackoff.multipliedBy((long) Math.pow(factor, (context.iteration() - 1)));
        return new BackoffDelay(firstBackoff, maxBackoffInterval, nextBackoff);
      };
    } else {
      return context -> {
        Duration prevBackoff = context.backoff() == null ? Duration.ZERO : context.backoff();
        Duration nextBackoff = prevBackoff.multipliedBy(factor);
        nextBackoff = nextBackoff.compareTo(firstBackoff) < 0 ? firstBackoff : nextBackoff;
        return new BackoffDelay(firstBackoff, maxBackoff, nextBackoff);
      };
    }
  }
}
