/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

/**
 * @param <T> the class that encapsulates the data for the profiling event context.
 */
public interface ProfilingDataProducer<T> {

  /**
   * Notifies a profiling event.
   *
   * @param profilerEventContext the contextual data for the emitted event.
   */
  void event(T profilerEventContext);

}
