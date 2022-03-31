/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.profiling;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingService;

/**
 * A {@link ProfilingService} that allows to perform some extra privileged operations.
 */
public interface PrivilegedProfilingService extends ProfilingService {

  /**
   * Registers a {@link ProfilingDataConsumer} dynamically.
   *
   * @param profilingDataConsumer the {@link ProfilingDataConsumer} to register.
   * @param <T>                   the {@link ProfilingEventContext} corresponding to the profiling event types the data consumer
   *                              listens to.
   */
  <T extends ProfilingEventContext> void registerProfilingDataConsumer(ProfilingDataConsumer<T> profilingDataConsumer);
}
