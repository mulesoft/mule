/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

/**
 * Adapts an {@link ExecutorCallback} into a {@link CompletionCallback}
 */
public class ExecutorCompletionCallbackAdapter implements CompletionCallback<Object, Object> {

  private final ExecutorCallback executorCallback;

  public ExecutorCompletionCallbackAdapter(ExecutorCallback executorCallback) {
    this.executorCallback = executorCallback;
  }

  @Override
  public void success(Result<Object, Object> result) {
    executorCallback.complete(result);
  }

  @Override
  public void error(Throwable e) {
    executorCallback.error(e);
  }
}
