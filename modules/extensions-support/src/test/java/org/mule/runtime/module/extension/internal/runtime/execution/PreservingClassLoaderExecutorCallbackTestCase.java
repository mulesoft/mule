/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.mockito.Mock;

@Issue("MULE-19097")
public class PreservingClassLoaderExecutorCallbackTestCase extends AbstractMuleTestCase {

  @Mock
  private CompletableComponentExecutor.ExecutorCallback executorCallback =
      mock(CompletableComponentExecutor.ExecutorCallback.class);

  @Test
  public void preserveClassLoaderOnError() {
    Reference<PreservingClassLoaderExecutorCallback> preservingClassLoaderExecutorCallbackReference = new Reference<>();

    ClassLoader onCreationClassLoader = mock(ClassLoader.class);
    withContextClassLoader(onCreationClassLoader, () -> {
      preservingClassLoaderExecutorCallbackReference.set(new PreservingClassLoaderExecutorCallback(executorCallback));
    });

    Reference<ClassLoader> onErrorClassLoaderRef = new Reference<>();
    doAnswer(ignored -> {
      onErrorClassLoaderRef.set(currentThread().getContextClassLoader());
      return null;
    }).when(executorCallback).error(any());

    ClassLoader anotherClassLoader = mock(ClassLoader.class);
    withContextClassLoader(anotherClassLoader, () -> {
      preservingClassLoaderExecutorCallbackReference.get().error(new NullPointerException());
    });

    assertThat(onErrorClassLoaderRef.get(), is(onCreationClassLoader));
  }

  @Test
  public void preserveClassLoaderOnComplete() {
    Reference<PreservingClassLoaderExecutorCallback> preservingClassLoaderExecutorCallbackReference = new Reference<>();

    ClassLoader onCreationClassLoader = mock(ClassLoader.class);
    withContextClassLoader(onCreationClassLoader, () -> {
      preservingClassLoaderExecutorCallbackReference.set(new PreservingClassLoaderExecutorCallback(executorCallback));
    });

    Reference<ClassLoader> onCompleteClassLoaderRef = new Reference<>();
    doAnswer(ignored -> {
      onCompleteClassLoaderRef.set(currentThread().getContextClassLoader());
      return null;
    }).when(executorCallback).complete(any());

    ClassLoader anotherClassLoader = mock(ClassLoader.class);
    withContextClassLoader(anotherClassLoader, () -> {
      preservingClassLoaderExecutorCallbackReference.get().complete(null);
    });

    assertThat(onCompleteClassLoaderRef.get(), is(onCreationClassLoader));
  }
}
