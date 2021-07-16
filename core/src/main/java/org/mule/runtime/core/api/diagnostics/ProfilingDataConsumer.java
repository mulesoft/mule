/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

import org.mule.api.annotation.Experimental;

import java.util.Set;
import java.util.function.Predicate;

/**
 * A consumer of profiler data.
 *
 * @since 4.4.0
 */
@Experimental
public interface ProfilingDataConsumer<T extends ProfilingEventContext> {

  /**
   * callback for consuming the profiling event.
   *
   * @param profilerEventIdentifier the profiling event id.
   * @param profilerEventContext    the profiler event context.
   */
  void onProfilingEvent(String profilerEventIdentifier, T profilerEventContext);

  /**
   * @return the {@link ProfilingEventType} {@link ProfilingDataConsumer} will listen to.
   */
  Set<ProfilingEventType<T>> profilerEventTypes();

  /**
   * @return the selector to indicate another filter for the events the data consumer will consume.
   */
  default Predicate<T> selector() {
    return ctx -> true;
  }

}
