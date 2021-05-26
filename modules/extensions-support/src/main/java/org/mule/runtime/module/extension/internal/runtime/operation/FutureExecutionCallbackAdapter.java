/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
