/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.retry.api;

import org.mule.api.annotation.NoImplement;

/**
 * This interface is a callback that allows actions to be performed after each retry attempt, such as firing notifications,
 * logging, etc.
 */
@NoImplement
public interface RetryNotifier {

  /** Called each time a retry attempt fails. */
  void onFailure(RetryContext context, Throwable e);

  /** Called when a retry attempt finally suceeds. */
  void onSuccess(RetryContext context);
}
