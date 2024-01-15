/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

import org.mule.api.annotation.NoImplement;

/**
 * A RetryPolicy takes some action each time an exception occurs and returns a {@link PolicyStatus} which indicates whether the
 * policy is exhausted or should continue to retry.
 */
@Deprecated
@NoImplement
public interface RetryPolicy extends org.mule.runtime.retry.api.policy.RetryPolicy {

  @Override
  PolicyStatus applyPolicy(Throwable cause);

}
