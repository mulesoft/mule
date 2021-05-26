/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.lang.Thread.currentThread;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import org.junit.Test;

public class DeferredExecutorCallbackTestCase {

  @Test
  public void completeIsNotDelegatedWhileDeferredCallbackIsNotClosed() throws Exception {
    TestExecutorCallback testExecutorCallback = new TestExecutorCallback();
    try (DeferredExecutorCallback deferredCallback = new DeferredExecutorCallback(testExecutorCallback)) {
      deferredCallback.complete(53);
      assertThat(testExecutorCallback.isCompleteCalled(), is(false));
    }
  }

  @Test
  public void errorIsNotDelegatedWhileDeferredCallbackIsNotClosed() throws Exception {
    TestExecutorCallback testExecutorCallback = new TestExecutorCallback();
    try (DeferredExecutorCallback deferredCallback = new DeferredExecutorCallback(testExecutorCallback)) {
      deferredCallback.error(new NullPointerException());
      assertThat(testExecutorCallback.isErrorCalled(), is(false));
    }
  }

  @Test
  public void completeIsDelegatedWhenDeferredCallbackIsClosed() throws Exception {
    TestExecutorCallback testExecutorCallback = new TestExecutorCallback();
    try (DeferredExecutorCallback deferredCallback = new DeferredExecutorCallback(testExecutorCallback)) {
      deferredCallback.complete(53);
    }
    assertThat(testExecutorCallback.isCompleteCalled(), is(true));
  }

  @Test
  public void errorIsDelegatedWhenDeferredCallbackIsClosed() throws Exception {
    TestExecutorCallback testExecutorCallback = new TestExecutorCallback();
    try (DeferredExecutorCallback deferredCallback = new DeferredExecutorCallback(testExecutorCallback)) {
      deferredCallback.error(new NullPointerException());
    }
    assertThat(testExecutorCallback.isErrorCalled(), is(true));
  }

  @Test
  public void completionClassLoaderIsPreserved() throws Exception {
    ClassLoader completionClassLoader = mock(ClassLoader.class);
    TestExecutorCallback testExecutorCallback = new TestExecutorCallback();
    try (DeferredExecutorCallback deferredCallback = new DeferredExecutorCallback(testExecutorCallback)) {
      withContextClassLoader(completionClassLoader, () -> deferredCallback.complete(53));
    }
    assertThat(testExecutorCallback.getCompleteClassLoader(), is(completionClassLoader));
  }

  @Test
  public void errorClassLoaderIsPreserved() throws Exception {
    ClassLoader errorClassLoader = mock(ClassLoader.class);
    TestExecutorCallback testExecutorCallback = new TestExecutorCallback();
    try (DeferredExecutorCallback deferredCallback = new DeferredExecutorCallback(testExecutorCallback)) {
      withContextClassLoader(errorClassLoader, () -> deferredCallback.error(new NullPointerException()));
    }
    assertThat(testExecutorCallback.getErrorClassLoader(), is(errorClassLoader));
  }

  private static class TestExecutorCallback implements ExecutorCallback {

    private ClassLoader completeClassLoader;

    private ClassLoader errorClassLoader;

    @Override
    public void complete(Object value) {
      completeClassLoader = currentThread().getContextClassLoader();
    }

    @Override
    public void error(Throwable e) {
      errorClassLoader = currentThread().getContextClassLoader();
    }

    boolean isCompleteCalled() {
      return completeClassLoader != null;
    }

    boolean isErrorCalled() {
      return errorClassLoader != null;
    }

    ClassLoader getCompleteClassLoader() {
      return completeClassLoader;
    }

    ClassLoader getErrorClassLoader() {
      return errorClassLoader;
    }
  }
}
