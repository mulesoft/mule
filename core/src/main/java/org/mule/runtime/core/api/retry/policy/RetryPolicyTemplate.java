/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.RetryNotifier;

import java.util.concurrent.Executor;

/**
 * A RetryPolicyTemplate creates a new {@link RetryPolicy} instance each time the retry goes into effect, thereby resetting any
 * state the policy may have (counters, etc.)
 * <p>
 * A {@link RetryNotifier} may be set in order to take action upon each retry attempt.
 */
@NoImplement
public interface RetryPolicyTemplate extends org.mule.runtime.retry.api.policy.RetryPolicyTemplate {

  @Override
  RetryPolicy createRetryInstance();

  @Override
  RetryNotifier getNotifier();

  @Override
  default void setNotifier(org.mule.runtime.retry.api.RetryNotifier retryNotifier) {
    // Nothing to do
  }

  void setNotifier(RetryNotifier retryNotifier);

  @Override
  default org.mule.runtime.retry.api.RetryContext execute(org.mule.runtime.retry.api.RetryCallback callback, Executor workManager)
      throws Exception {
    // Nothing to do
    return null;
  }

  /**
   * Applies the retry policy by performing a blocking action.
   *
   * @param callback    a callback with the logic to be executed on each retry
   * @param workManager the executor on which the retry operations are to be executed
   * @return a {@link RetryContext}
   */
  RetryContext execute(RetryCallback callback, Executor workManager) throws Exception;

}
