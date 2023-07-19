/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.service.scheduler;

import org.mule.runtime.api.scheduler.Scheduler;

import java.util.TimeZone;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MockScheduler extends ScheduledThreadPoolExecutor implements Scheduler {

  public MockScheduler() {
    super(1);
  }

  public MockScheduler(String name) {
    super(1, new org.mule.runtime.core.api.util.concurrent.NamedThreadFactory(name));
  }
  
  @Override
  public void stop() {
    shutdownNow();
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression) {
    throw new UnsupportedOperationException("Cron expression scheduling is not supported in unit tests. You need the productive service implementation.");
  }

  @Override
  public ScheduledFuture<?> scheduleWithCronExpression(Runnable command, String cronExpression, TimeZone timeZone) {
    throw new UnsupportedOperationException("Cron expression scheduling is not supported in unit tests. You need the productive service implementation.");
  }

  public String getName() {
    return MockScheduler.class.getSimpleName();
  }
}
