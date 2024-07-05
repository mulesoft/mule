/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.util.MuleSystemProperties.DISABLE_NATIVE_LIBRARIES_FOLDER_DELETION_GC_CALL_PROPERTY;

import static java.lang.Boolean.getBoolean;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

public class RetryScheduledFolderDeletionTask implements Runnable {

  private final ScheduledExecutorService scheduler;
  private final int maxAttempts;
  private final AtomicInteger attempts;
  private final NativeLibrariesFolderDeletion folderDeletion;
  private static final Logger LOGGER = getLogger(RetryScheduledFolderDeletionTask.class);
  private static final boolean DISABLE_NATIVE_LIBRARIES_FOLDER_DELETION_GC_CALL =
      getBoolean(DISABLE_NATIVE_LIBRARIES_FOLDER_DELETION_GC_CALL_PROPERTY);


  public RetryScheduledFolderDeletionTask(ScheduledExecutorService scheduler, int maxAttempts,
                                          NativeLibrariesFolderDeletion folderDeletion) {
    this.scheduler = scheduler;
    this.maxAttempts = maxAttempts;
    this.attempts = new AtomicInteger(0);
    this.folderDeletion = folderDeletion;
  }

  @Override
  public void run() {
    int attempt = attempts.incrementAndGet();

    if (!DISABLE_NATIVE_LIBRARIES_FOLDER_DELETION_GC_CALL && attempt == maxAttempts) {
      System.gc();
    }

    if (performAction()) {
      scheduler.shutdown();
    } else {
      if (attempt >= maxAttempts) {
        LOGGER.error("Failed to perform the action. No further retries will be made.");
        scheduler.shutdown();
      } else {
        LOGGER.warn("Attempt {}. Failed to perform the action. Retrying...", attempt);
      }
    }
  }

  private boolean performAction() {
    return folderDeletion.doAction();
  }
}
