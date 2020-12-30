/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CompletableOperationExecutorFactoryTestCase extends AbstractMuleTestCase {

  private CompletableOperationExecutorFactory<String, ComponentModel> factory;
  CompletableComponentExecutor<ComponentModel> executor;
  ExecutionContextAdapter<ComponentModel> executionContext;
  CompletableComponentExecutor.ExecutorCallback callback;
  Reference<CompletionCallback<Object, Object>> completionCallbackRef;
  Reference<ClassLoader> onErrorClassLoaderRef;
  Reference<ClassLoader> onCompleteClassLoaderRef;

  @Before
  public void setup() {
    factory = new CompletableOperationExecutorFactory<>(String.class, String.class.getMethods()[0]);
    executor = factory.createExecutor(mock(OperationModel.class), emptyMap());

    executionContext = mock(ExecutionContextAdapter.class);
    callback = mock(CompletableComponentExecutor.ExecutorCallback.class);

    completionCallbackRef = new Reference<>();
    when(executionContext.setVariable(eq(COMPLETION_CALLBACK_CONTEXT_PARAM), any(CompletionCallback.class)))
        .thenAnswer(invocation -> {
          completionCallbackRef.set(invocation.getArgument(1));
          return null;
        });

    onErrorClassLoaderRef = new Reference<>();
    doAnswer(ignored -> {
      onErrorClassLoaderRef.set(currentThread().getContextClassLoader());
      return null;
    }).when(callback).error(any());

    onCompleteClassLoaderRef = new Reference<>();
    doAnswer(ignored -> {
      onCompleteClassLoaderRef.set(currentThread().getContextClassLoader());
      return null;
    }).when(callback).complete(any());
  }

  @Test
  public void preserveClassLoaderOnError() {
    // Given a non-blocking operation executed with certain classloader.
    ClassLoader executionClassLoader = mock(ClassLoader.class);
    withContextClassLoader(executionClassLoader, () -> executor.execute(executionContext, callback));

    // When the completion callback is completed with error using another classloader.
    ClassLoader anotherClassLoader = mock(ClassLoader.class);
    withContextClassLoader(anotherClassLoader, () -> completionCallbackRef.get().error(new NullPointerException()));

    // Then the real on error method is called using the execution classloader.
    assertThat(onErrorClassLoaderRef.get(), is(executionClassLoader));
  }

  @Test
  public void preserveClassLoaderOnComplete() {
    // Given a non-blocking operation executed with certain classloader.
    ClassLoader executionClassLoader = mock(ClassLoader.class);
    withContextClassLoader(executionClassLoader, () -> executor.execute(executionContext, callback));

    // When the completion callback is completed using another classloader.
    ClassLoader anotherClassLoader = mock(ClassLoader.class);
    withContextClassLoader(anotherClassLoader, () -> completionCallbackRef.get().success(null));

    // Then the real complete method is called using the execution classloader.
    assertThat(onCompleteClassLoaderRef.get(), is(executionClassLoader));
  }
}
