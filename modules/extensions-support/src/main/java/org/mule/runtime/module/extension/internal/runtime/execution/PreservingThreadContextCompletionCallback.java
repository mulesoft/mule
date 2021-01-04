/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.Thread.currentThread;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

import java.util.Map;

import org.slf4j.MDC;

public class PreservingThreadContextCompletionCallback<T, A> implements CompletionCallback<T, A> {

  private final CompletionCallback<T, A> delegate;
  private final ClassLoader classLoader;
  private final Map<String, String> mdc;

  public PreservingThreadContextCompletionCallback(CompletionCallback<T, A> delegate) {
    this.delegate = delegate;
    this.classLoader = currentThread().getContextClassLoader();
    this.mdc = MDC.getCopyOfContextMap();
  }

  @Override
  public void success(Result<T, A> result) {
    try (ThreadContext ignored = new ThreadContext(classLoader, mdc)) {
      delegate.success(result);
    }
  }

  @Override
  public void error(Throwable e) {
    try (ThreadContext ignored = new ThreadContext(classLoader, mdc)) {
      delegate.error(e);
    }
  }

  private static class ThreadContext implements AutoCloseable {

    private final Thread currentThread;

    private final ClassLoader innerClassLoader;
    private final Map<String, String> innerMDC;

    private final ClassLoader outerClassLoader;
    private final Map<String, String> outerMDC;

    ThreadContext(ClassLoader classLoader, Map<String, String> mdc) {
      currentThread = currentThread();

      innerClassLoader = classLoader;
      innerMDC = mdc;

      outerClassLoader = currentThread.getContextClassLoader();
      outerMDC = MDC.getCopyOfContextMap();

      MDC.setContextMap(innerMDC);
      setContextClassLoader(currentThread, outerClassLoader, innerClassLoader);
    }

    @Override
    public void close() {
      try {
        setContextClassLoader(currentThread, innerClassLoader, outerClassLoader);
      } finally {
        MDC.setContextMap(outerMDC);
      }
    }

    private static void setContextClassLoader(Thread thread, ClassLoader currentClassLoader, ClassLoader newClassLoader) {
      if (currentClassLoader != newClassLoader) {
        thread.setContextClassLoader(newClassLoader);
      }
    }
  }
}
