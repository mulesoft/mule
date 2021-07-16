/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

import org.mule.api.annotation.Experimental;

/**
 * Service that provides resources for profiling.
 *
 * @since 4.4.0
 */
@Experimental
public interface DiagnosticsService {


  /**
   * Returns a data producer to notify events for a profiler event type
   *
   * @param profilingEventType the profiler event type for the {@link ProfilingDataProducer}
   * @return the profiler data producer
   */
  <T extends ProfilingEventContext> ProfilingDataProducer<T> getProfilingDataProducer(ProfilingEventType<T> profilingEventType);
}
