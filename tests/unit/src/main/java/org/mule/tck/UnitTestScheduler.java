/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.runtime.core.api.scheduler.Scheduler;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * {@link Scheduler} implementation to be used in unit tests. Provides a thread pool with just 2 threads.
 *
 * @since 4.0
 */
public class UnitTestScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

  public UnitTestScheduler() {
    super(2, new NamedThreadFactory("UnitTestScheduler"), new AbortPolicy());
  }

  @Override
  public void stop(long gracefulShutdownTimeoutSecs, TimeUnit unit) {
    // Nothing to do
  }

}
