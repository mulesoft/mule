/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

public class RetryScheduledFileDeletionTask implements Runnable {

  private final ScheduledExecutorService scheduler;
  private final int maxAttempts;
  private final AtomicInteger attempts;
  private final NativeLibrariesFileDeletion fileDeletion;
  private static final Logger LOGGER = getLogger(RetryScheduledFileDeletionTask.class);

  public RetryScheduledFileDeletionTask(ScheduledExecutorService scheduler, int maxAttempts,
                                        NativeLibrariesFileDeletion fileDeletion) {
    this.scheduler = scheduler;
    this.maxAttempts = maxAttempts;
    this.attempts = new AtomicInteger(0);
    this.fileDeletion = fileDeletion;
  }

  @Override
  public void run() {
    int attempt = attempts.incrementAndGet();
    if (performAction()) {
      scheduler.shutdown();
    } else {
      if (attempt >= maxAttempts) {
        LOGGER.error("Failed to perform the action. No further retries will be made.");
        scheduler.shutdown();
      } else {
        LOGGER.warn(format("Attempt %s. Failed to perform the action. Retrying...", attempt));
      }
    }
  }

  private boolean performAction() {
    return fileDeletion.doAction();
  }
}
