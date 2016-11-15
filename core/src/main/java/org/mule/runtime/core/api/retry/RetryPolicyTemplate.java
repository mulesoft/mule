/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry;

import java.util.Map;
import java.util.concurrent.Executor;


/**
 * A RetryPolicyTemplate creates a new {@link RetryPolicy} instance each time the retry goes into effect, thereby resetting any
 * state the policy may have (counters, etc.)
 * 
 * A {@link RetryNotifier} may be set in order to take action upon each retry attempt.
 */
public interface RetryPolicyTemplate {

  RetryPolicy createRetryInstance();

  Map<Object, Object> getMetaInfo();

  void setMetaInfo(Map<Object, Object> metaInfo);

  RetryNotifier getNotifier();

  void setNotifier(RetryNotifier retryNotifier);

  RetryContext execute(RetryCallback callback, Executor workManager) throws Exception;
}
