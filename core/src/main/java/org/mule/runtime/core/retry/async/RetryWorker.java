/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.retry.async;

import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.util.concurrent.Latch;

import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link Runnable} implementation used when executing a {@link RetryPolicyTemplate} in a separate thread.
 */
public class RetryWorker implements Runnable {

  protected transient final Logger logger = LoggerFactory.getLogger(RetryWorker.class);

  private final RetryCallback callback;
  private final Executor workManager;
  private Exception exception = null;
  private final FutureRetryContext context = new FutureRetryContext();
  private final RetryPolicyTemplate delegate;
  private Latch startLatch;

  public RetryWorker(RetryPolicyTemplate delegate, RetryCallback callback, Executor workManager) {
    this(delegate, callback, workManager, null);
  }

  public RetryWorker(RetryPolicyTemplate delegate, RetryCallback callback, Executor workManager, Latch startLatch) {
    this.callback = callback;
    this.workManager = workManager;
    this.delegate = delegate;
    this.startLatch = startLatch;
    if (this.startLatch == null) {
      this.startLatch = new Latch();
      this.startLatch.countDown();
    }
  }

  @Override
  public void run() {
    try {
      startLatch.await();
    } catch (InterruptedException e) {
      logger.warn("Retry thread interrupted for callback: " + callback.getWorkDescription());
      return;
    }
    try {
      context.setDelegateContext(delegate.execute(callback, workManager));
    } catch (Exception e) {
      this.exception = e;
      logger.error("Error retrying work", e);

    }
  }

  public Exception getException() {
    return exception;
  }

  public FutureRetryContext getRetryContext() {
    return context;
  }
}
