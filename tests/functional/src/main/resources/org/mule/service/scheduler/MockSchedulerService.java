/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler;

import static org.mule.runtime.core.api.scheduler.ThreadType.CUSTOM;

import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.scheduler.ThreadType;

public class MockSchedulerService implements SchedulerService {

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    return new MockScheduler();
  }

  @Override
  public Scheduler ioScheduler() {
    return new MockScheduler();
  }

  @Override
  public Scheduler computationScheduler() {
    return new MockScheduler();
  }
  
  @Override
  public customScheduler(int corePoolSize, String name) {
    return new MockScheduler();
  }
  
  @Override
  public ThreadType currentThreadType() {
    return CUSTOM;
  }
}
