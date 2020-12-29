/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
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
  private final ClassLoader classLoader;

  public ExecutorCompletionCallbackAdapter(ExecutorCallback executorCallback) {
    this.executorCallback = executorCallback;
    this.classLoader = currentThread().getContextClassLoader();
  }

  @Override
  public void success(Result<Object, Object> result) {
    Thread currentThread = currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    try {
      executorCallback.complete(result);
    } finally {
      setContextClassLoader(currentThread, currentClassLoader, classLoader);
    }
  }

  @Override
  public void error(Throwable e) {
    Thread currentThread = currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    try {
      executorCallback.error(e);
    } finally {
      setContextClassLoader(currentThread, currentClassLoader, classLoader);
    }
  }
}
