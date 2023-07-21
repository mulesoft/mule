/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
