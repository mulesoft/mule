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

public class CompletionCallbackFacade<T, A> implements CompletionCallback<T, A> {

  private final ExecutorCallback delegate;

  public CompletionCallbackFacade(ExecutorCallback delegate) {
    this.delegate = delegate;
  }

  @Override
  public void success(Result<T, A> result) {
    delegate.complete(result);
  }

  @Override
  public void error(Throwable e) {
    delegate.error(e);
  }
}
