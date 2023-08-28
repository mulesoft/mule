/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import java.util.concurrent.CompletableFuture;

/**
 * ExecutorCallback adapter which delegates to the corresponding methods in a CompletableFuture.
 */
class FutureExecutionCallbackAdapter implements ExecutorCallback {

  private final CompletableFuture<Object> future;

  FutureExecutionCallbackAdapter(CompletableFuture<Object> future) {
    this.future = future;
  }

  @Override
  public void complete(Object value) {
    future.complete(value);
  }

  @Override
  public void error(Throwable e) {
    future.completeExceptionally(e);
  }
}
