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
 * Defines the resources provided by an orchestrator for handling the schedulers used for dispatching an event to a processor and
 * for returning the execution to the main flow.
 *
 * @since 4.4.0, 4.3.1
 */
public interface ExecutionOrchestrator {

  /**
   * @return the {@link ExecutorService} scheduler that will be used in the processor execution.
   */
  ExecutorService getDispatcherScheduler();

  /**
   * @return the {@link ExecutorService} for returning the processing to the main flow after the response is received.
   */
  ExecutorService getCallbackScheduler();

  Scheduler getContextScheduler();
}
