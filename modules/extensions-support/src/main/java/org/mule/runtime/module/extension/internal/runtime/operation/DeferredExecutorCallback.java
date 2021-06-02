/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.api.util.ClassUtils.setContextClassLoader;
import org.mule.runtime.core.internal.execution.IsolateCurrentTransactionInterceptor;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

/**
 * Class used to defer an executor callback until this is closed, so the execution can be intercepted without calling the executor
 * callback methods inside the interception. This is needed because the completion callback of the blocking operations call the
 * actual execution from the next operation, and therefore if it isn't deferred, then the execution template of the first
 * operation doesn't finish before starting the next execution template.
 *
 * One symptom of that template "nesting" (See MULE-19431 for reference) is that an operation with
 * transactionalAction="ALWAYS_BEGIN" couldn't be executed after another operation with transactionalAction="NOT_SUPPORTED"
 * operation because the NOT_SUPPORTED operation isolates the current transaction in the first part of its execution template (see
 * {@link IsolateCurrentTransactionInterceptor}), the completion callback calls the execution template of the ALWAYS_BEGIN
 * operation, and this second would try to join to the current transaction before the second part of the aforementioned
 * interceptor.
 *
 * <code>
 *   ExecutorCallback delegate;
 *
 *   try (DeferredExecutorCallback deferredCallback = new DeferredExecutorCallback(delegate)) {
 *     withSomeInterceptionLogic(() -> {
 *       deferredCallback.complete(someValue);
 *       // At this point, the delegate.complete(someValue) hasn't been called yet.
 *     }
 *   }
 *
 *   // When the execution goes out of the try, delegate.complete(someValue) is called. It's useful because the
 *   // interception logic isn't applied here.
 * </code>
 *
 * The methods of this class are synchronized because the callback completion can be invoked from other thread than the close
 * method. This shouldn't introduce a performance degradation because they're only called from two threads, and only once.
 */
class DeferredExecutorCallback implements ExecutorCallback, AutoCloseable {

  private Throwable error = null;
  private boolean isErrorCalled = false;

  private Object result = null;
  private boolean isCompleteCalled = false;

  private final ExecutorCallback delegate;

  private boolean isEnabled = false;
  private ClassLoader savedClassLoader = null;

  DeferredExecutorCallback(ExecutorCallback delegate) {
    this.delegate = delegate;
  }

  @Override
  public synchronized void complete(Object value) {
    isCompleteCalled = true;

    if (isEnabled) {
      delegate.complete(value);
    } else {
      this.result = value;
      this.savedClassLoader = currentThread().getContextClassLoader();
    }
  }

  @Override
  public synchronized void error(Throwable e) {
    isErrorCalled = true;

    if (isEnabled) {
      delegate.error(e);
    } else {
      this.error = e;
      this.savedClassLoader = currentThread().getContextClassLoader();
    }
  }

  @Override
  public synchronized void close() throws Exception {
    isEnabled = true;

    if (!isErrorCalled && !isCompleteCalled) {
      return;
    }

    Thread currentThread = currentThread();
    ClassLoader outerClassLoader = currentThread.getContextClassLoader();
    setContextClassLoader(currentThread, outerClassLoader, savedClassLoader);
    try {
      callDelegateMethod();
    } finally {
      setContextClassLoader(currentThread, savedClassLoader, outerClassLoader);
    }
  }

  private void callDelegateMethod() {
    if (isErrorCalled) {
      delegate.error(error);
    }
    if (isCompleteCalled) {
      delegate.complete(result);
    }
  }
}
