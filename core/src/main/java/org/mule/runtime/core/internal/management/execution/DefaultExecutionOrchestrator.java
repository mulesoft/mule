/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.management.execution;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;

import java.util.concurrent.ExecutorService;

import static org.mule.runtime.core.internal.util.rx.ImmediateScheduler.IMMEDIATE_SCHEDULER;

/**
 * A basic execution orchestrator that does not perform any switch on dispatching an event to a processor or for returning the
 * event to the main flow. An immediate scheduler is used.
 * 
 * @since 4.4.0, 4.3.1
 */
public class DefaultExecutionOrchestrator implements ExecutionOrchestrator {

  private static ExecutionOrchestrator instance = new DefaultExecutionOrchestrator();

  private static Scheduler scheduler = IMMEDIATE_SCHEDULER;

  private DefaultExecutionOrchestrator() {}

  public static ExecutionOrchestrator getInstance() {
    return instance;
  }

  @Override
  public ExecutorService getDispatcherScheduler() {
    return scheduler;
  }

  @Override
  public ExecutorService getCallbackScheduler() {
    return scheduler;
  }

  @Override
  public Scheduler getContextScheduler() {
    return scheduler;
  }
}
