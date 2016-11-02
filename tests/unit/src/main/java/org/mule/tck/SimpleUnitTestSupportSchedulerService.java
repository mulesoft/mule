/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;

import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

/**
 * {@link SchedulerService} implementation that provides a shared {@link SimpleUnitTestSupportScheduler}.
 *
 * @since 4.0
 */
public class SimpleUnitTestSupportSchedulerService implements SchedulerService, Stoppable {

  private SimpleUnitTestSupportScheduler scheduler =
      new SimpleUnitTestSupportScheduler(2, new NamedThreadFactory(SimpleUnitTestSupportScheduler.class.getSimpleName()),
                                         new AbortPolicy());

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Scheduler cpuLightScheduler() {
    return new SimpleUnitTestSupportLifecycleSchedulerDecorator(scheduler);
  }

  @Override
  public Scheduler ioScheduler() {
    return new SimpleUnitTestSupportLifecycleSchedulerDecorator(scheduler);
  }

  @Override
  public Scheduler computationScheduler() {
    return new SimpleUnitTestSupportLifecycleSchedulerDecorator(scheduler);
  }

  @Override
  public void stop() throws MuleException {
    scheduler.shutdownNow();
  }

  public int getScheduledTasks() {
    return scheduler.getScheduledTasks();
  }
}
