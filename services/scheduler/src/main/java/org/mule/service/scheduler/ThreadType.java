/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;

/**
 * Represents the type of work that a {@link Thread} owned by a {@link Scheduler} is configured to perform.
 *
 * @since 4.0
 */
public enum ThreadType {

  /**
   * The type for {@link Thread}s managed by {@link Scheduler}s obtained with {@link SchedulerService#ioScheduler()}.
   */
  IO("io"),

  /**
   * The type for {@link Thread}s managed by {@link Scheduler}s obtained with {@link SchedulerService#computationScheduler()}.
   */
  CPU_INTENSIVE("cpuIntensive"),

  /**
   * The type for {@link Thread}s managed by {@link Scheduler}s obtained with {@link SchedulerService#cpuLightScheduler()}.
   */
  CPU_LIGHT("cpuLight"),

  /**
   * The type for {@link Thread}s managed by custom {@link Scheduler}s.
   */
  CUSTOM("custom"),

  /**
   * The type for {@link Thread}s not managed by {@link Scheduler}s obtained from {@link SchedulerService}.
   */
  UNKNOWN("unknown");

  private String name;

  private ThreadType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
