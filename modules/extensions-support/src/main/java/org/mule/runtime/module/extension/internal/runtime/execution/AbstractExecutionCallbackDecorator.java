/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

public abstract class AbstractExecutionCallbackDecorator implements ExecutorCallback {

  protected final ExecutorCallback delegate;

  public AbstractExecutionCallbackDecorator(ExecutorCallback delegate) {
    this.delegate = delegate;
  }

  @Override
  public void complete(Object value) {
    delegate.complete(value);
  }

  @Override
  public void error(Throwable e) {
    delegate.error(e);
  }
}
