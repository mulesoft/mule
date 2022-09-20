/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.internal.memory.management;

import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.profiling.ProfilingService;

/**
 * A {@link MemoryManagementService} that can profile the usage of memory resources using the {@link ProfilingService} set.
 */
public interface ProfiledMemoryManagementService extends MemoryManagementService {

  /**
   * The {@link ProfilingService} used by the implementation for profiling the memory resources usage.
   *
   * @param profilingService the {@link ProfilingService} used by the implementation to track/profile resources.
   */
  void setProfilingService(ProfilingService profilingService);
}
