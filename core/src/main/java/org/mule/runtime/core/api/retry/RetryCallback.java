/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.retry;

import org.mule.runtime.core.api.retry.policy.RetryPolicy;

/**
 * This is the main Retry SPI. The code inside the {@link #doWork} method is what will actually get <u>retried</u> according to
 * the {@link RetryPolicy} that has been configured. Note that retries can be wrapped in a transaction to ensure the work is
 * atomic.
 */
public interface RetryCallback {

  void doWork(RetryContext context) throws Exception;

  String getWorkDescription();

  /**
   * @return the object for which the retry of the work is being done.
   */
  Object getWorkOwner();
}
