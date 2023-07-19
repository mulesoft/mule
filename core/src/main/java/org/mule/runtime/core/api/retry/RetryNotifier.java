/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.retry;

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
