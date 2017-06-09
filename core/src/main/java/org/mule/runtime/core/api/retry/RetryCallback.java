/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
