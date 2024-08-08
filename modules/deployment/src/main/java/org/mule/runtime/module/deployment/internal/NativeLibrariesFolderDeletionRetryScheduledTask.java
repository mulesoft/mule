/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_NATIVE_LIBRARIES_FOLDER_DELETION_GC_CALL_PROPERTY;

import static java.lang.Boolean.parseBoolean;
import static java.lang.System.getProperty;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

public class NativeLibrariesFolderDeletionRetryScheduledTask implements Runnable, ActionTask {

  private final ScheduledExecutorService scheduler;
  private final int maxAttempts;
  private final AtomicInteger attempts;
  private final ActionTask actionTask;
  private final RetryScheduledFolderDeletionTaskLogger logger;

  private static final Logger LOGGER = getLogger(NativeLibrariesFolderDeletionRetryScheduledTask.class);
  private static final boolean DISABLE_NATIVE_LIBRARIES_FOLDER_DELETION_GC_CALL =
      parseBoolean(getProperty(DISABLE_NATIVE_LIBRARIES_FOLDER_DELETION_GC_CALL_PROPERTY, "false"));

  public NativeLibrariesFolderDeletionRetryScheduledTask(ScheduledExecutorService scheduler, int maxAttempts,
                                                         ActionTask actionTask,
                                                         RetryScheduledFolderDeletionTaskLogger logger) {
    this.scheduler = scheduler;
    this.maxAttempts = maxAttempts;
    this.attempts = new AtomicInteger(0);
    this.actionTask = actionTask;
    this.logger = logger;
  }

  public NativeLibrariesFolderDeletionRetryScheduledTask(ScheduledExecutorService scheduler, int maxAttempts,
                                                         ActionTask actionTask) {
    this(scheduler, maxAttempts, actionTask, new Log4jWrapperRetryScheduledFolderDeletionTaskLogger(LOGGER));
  }

  @Override
  public void run() {
    int attempt = attempts.incrementAndGet();

    boolean secondToLastAttempt = attempt == maxAttempts - 1;
    if (!DISABLE_NATIVE_LIBRARIES_FOLDER_DELETION_GC_CALL && secondToLastAttempt) {
      System.gc();
      logger.debug("Attempt {}. System.gc() executed.", attempt);
    }

    if (tryAction()) {
      scheduler.shutdown();
    } else {
      if (attempt >= maxAttempts) {
        logger.error("Failed to perform the action. No further retries will be made.");
        scheduler.shutdown();
      } else {
        logger.warn("Attempt {}. Failed to perform the action. Retrying...", attempt);
      }
    }
  }

  @Override
  public boolean tryAction() {
    return actionTask.tryAction();
  }
}
