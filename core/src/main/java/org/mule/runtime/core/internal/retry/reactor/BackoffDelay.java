/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

import java.time.Duration;

public class BackoffDelay {

  static final BackoffDelay ZERO = new BackoffDelay(Duration.ZERO);

  final Duration min;
  final Duration max;
  final Duration delay;

  public BackoffDelay(Duration fixedBackoff) {
    this(fixedBackoff, fixedBackoff, fixedBackoff);
  }

  public BackoffDelay(Duration min, Duration max, Duration delay) {
    this.min = min;
    this.max = max;
    this.delay = delay;
  }

  public Duration minDelay() {
    return min;
  }

  public Duration maxDelay() {
    return max;
  }

  public Duration delay() {
    return delay;
  }

}
