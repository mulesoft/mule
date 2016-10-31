/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler;

import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;

/**
 * @since 4.0
 */
public class MockSchedulerService implements SchedulerService {

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    return null;
  }

  @Override
  public Scheduler ioScheduler() {
    return null;
  }

  @Override
  public Scheduler computationScheduler() {
    return null;
  }
}
