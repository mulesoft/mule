/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.scheduler;

import org.mule.runtime.core.api.scheduler.Scheduler;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MockScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

  public MockScheduler() {
    super(1);
  }

  @Override
  public void stop(long gracefulShutdownTimeoutSecs, TimeUnit unit) {
    // Nothing to do.
  }
}
