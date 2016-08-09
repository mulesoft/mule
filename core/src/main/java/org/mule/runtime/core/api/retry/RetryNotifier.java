/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry;

/**
 * This interface is a callback that allows actions to be performed after each retry attempt, such as firing notifications,
 * logging, etc.
 */
public interface RetryNotifier {

  /** Called each time a retry attempt fails. */
  public void onFailure(RetryContext context, Throwable e);

  /** Called when a retry attempt finally suceeds. */
  public void onSuccess(RetryContext context);
}
