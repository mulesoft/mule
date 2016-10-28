/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;

/**
 * {@link SchedulerService} implementation that provides a shared {@link UnitTestScheduler}.
 *
 * @since 4.0
 */
public class UnitTestSchedulerService implements SchedulerService, Stoppable {

  private Scheduler scheduler = new UnitTestScheduler();

  @Override
  public String getName() {
    return "UnitTestSchedulerService";
  }

  @Override
  public Scheduler cpuLightScheduler() {
    return scheduler;
  }

  @Override
  public Scheduler ioScheduler() {
    return scheduler;
  }

  @Override
  public Scheduler computationScheduler() {
    return scheduler;
  }

  @Override
  public void stop() throws MuleException {
    scheduler.shutdownNow();
  }
}
