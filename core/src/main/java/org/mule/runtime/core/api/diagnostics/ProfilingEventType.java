/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.diagnostics;

/**
 * The profiling event type.
 *
 * @param <T> the {@link ProfilingEventContext} associated to the type.
 */
public interface ProfilingEventType<T extends ProfilingEventContext> {

  /**
   * @return the profiling event type name.
   */
  String getProfilingEventName();
}
