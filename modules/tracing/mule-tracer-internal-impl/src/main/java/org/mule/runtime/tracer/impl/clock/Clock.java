/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.clock;

import static org.mule.runtime.tracer.impl.clock.SystemNanoTimeClock.getInstance;

/**
 * A clock used for tracing and measure the duration of {@link org.mule.runtime.tracer.api.span.InternalSpan}.
 *
 * @since 4.5.0
 */
public interface Clock {

  /**
   * @return default implementation of clock.
   */
  static Clock getDefault() {
    return getInstance();
  }

  /**
   * @return the current epoch timestamp in nanos from this clock.
   */
  long now();
}
