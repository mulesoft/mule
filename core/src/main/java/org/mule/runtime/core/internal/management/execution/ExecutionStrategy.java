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
 * Encapsulates logic to define which dispatcher scheduler, which callback scheduler and which context scheduler is used when a
 * processing strategy is applied.
 * 
 * @since 4.4.0, 4.3.1
 */
public interface ExecutionStrategy {

  /**
   * @return the @{@link ExecutorService} used to dispatch an event before a component processor.
   */
  ExecutorService getDispatcherScheduler();

  /**
   * @return the @{@link ExecutorService} used to dispatch an event to the main flow after a component execution.
   */
  ExecutorService getCallbackScheduler();

  /**
   * @return the @{@link Scheduler} to be used in the context of a component execution.
   */
  Scheduler getContextScheduler();
}
