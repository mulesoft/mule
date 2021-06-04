/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.execution;

import org.mule.runtime.api.scheduler.Scheduler;

import java.util.concurrent.ExecutorService;

/**
 * Implementation of a execution orchestrator that delegates the election of a scheduler in a @{@link ExecutionStrategy}
 *
 * @since 4.4.0, 4.3.1
 */
public class StreamExecutionOrchestrator implements ExecutionOrchestrator {

  private final ExecutionStrategy executionStrategy;

  public StreamExecutionOrchestrator(ExecutionStrategy executionStrategy) {
    this.executionStrategy = executionStrategy;
  }

  @Override
  public ExecutorService getDispatcherScheduler() {
    return executionStrategy.getDispatcherScheduler();
  }

  @Override
  public ExecutorService getCallbackScheduler() {
    return executionStrategy.getCallbackScheduler();
  }

  @Override
  public Scheduler getContextScheduler() {
    return executionStrategy.getContextScheduler();
  }

}
