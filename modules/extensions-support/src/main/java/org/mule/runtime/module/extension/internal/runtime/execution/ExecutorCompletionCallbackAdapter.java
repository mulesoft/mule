/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

/**
 * Adapts an {@link ExecutorCallback} into a {@link CompletionCallback}
 *
 * @since 4.3
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
