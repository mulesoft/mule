/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.internal.management.execution.ExecutionStrategy;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An Implementation of a @{@link ExecutionStrategy} which simply uses the schedulers defined by default.
 * 
 * @since 4.4.0, 4.3.1
 */
public class StreamExecutionStrategy implements ExecutionStrategy {

  private final ExecutorService dispatcherScheduler;
  private final ExecutorService flowDispatcherScheduler;
  private final Scheduler contextProcesorScheduler;

  public StreamExecutionStrategy(ExecutorService dispatcherScheduler, ExecutorService flowDispatcherScheduler,
                                 Scheduler contextProcessorScheduler) {
    this.dispatcherScheduler = dispatcherScheduler;
    this.flowDispatcherScheduler = flowDispatcherScheduler;
    this.contextProcesorScheduler = contextProcessorScheduler;
  }

  @Override
  public ExecutorService getDispatcherScheduler() {
    return dispatcherScheduler;
  }

  @Override
  public ExecutorService getCallbackScheduler() {
    return flowDispatcherScheduler;
  }

  @Override
  public Scheduler getContextScheduler() {
    return contextProcesorScheduler;
  }
}
