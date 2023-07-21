/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.service.scheduler;

import org.mule.runtime.api.scheduler.SchedulerView;

import java.util.TimeZone;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class MockSchedulerView implements SchedulerView {

  public MockSchedulerView() {
  }

  public boolean isShutdown() {
    return false;
  }

  public boolean isTerminated() {
    return false;
  }

  public String getName() {
    return MockSchedulerView.class.getSimpleName();
  }
}
