/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
