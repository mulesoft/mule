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

public class PreservingClassLoaderExecutorCallback implements ExecutorCallback {

  private final ExecutorCallback delegate;
  private final ClassLoader classLoader;

  public PreservingClassLoaderExecutorCallback(ExecutorCallback delegate) {
    this.delegate = delegate;
    this.classLoader = currentThread().getContextClassLoader();
  }

  @Override
  public void complete(Object value) {
    Thread currentThread = currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    setContextClassLoader(currentThread, currentClassLoader, classLoader);
    try {
      delegate.complete(value);
    } finally {
      setContextClassLoader(currentThread, classLoader, currentClassLoader);
    }
  }

  @Override
  public void error(Throwable e) {
    Thread currentThread = currentThread();
    ClassLoader currentClassLoader = currentThread.getContextClassLoader();
    setContextClassLoader(currentThread, currentClassLoader, classLoader);
    try {
      delegate.error(e);
    } finally {
      setContextClassLoader(currentThread, classLoader, currentClassLoader);
    }
  }
}
