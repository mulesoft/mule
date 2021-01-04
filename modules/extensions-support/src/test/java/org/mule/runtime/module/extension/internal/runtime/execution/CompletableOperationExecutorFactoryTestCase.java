/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextProperties.COMPLETION_CALLBACK_CONTEXT_PARAM;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

@Issue("MULE-19097")
public class CompletableOperationExecutorFactoryTestCase extends AbstractMuleTestCase {

  private CompletableComponentExecutor<ComponentModel> executor;
  private ExecutionContextAdapter<ComponentModel> executionContext;
  private CompletableComponentExecutor.ExecutorCallback callback;
  private Reference<CompletionCallback<Object, Object>> completionCallbackRef;
  private Reference<ClassLoader> onErrorClassLoaderRef;
  private Reference<ClassLoader> onCompleteClassLoaderRef;
  private Reference<Map<String, String>> onErrorMDC;
  private Reference<Map<String, String>> onCompleteMDC;

  @Before
  public void setup() {
    CompletableOperationExecutorFactory<String, ComponentModel> factory =
        new CompletableOperationExecutorFactory<>(String.class, String.class.getMethods()[0]);
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
    onErrorMDC = new Reference<>();
    doAnswer(ignored -> {
      onErrorClassLoaderRef.set(currentThread().getContextClassLoader());
      onErrorMDC.set(MDC.getCopyOfContextMap());
      return null;
    }).when(callback).error(any());

    onCompleteClassLoaderRef = new Reference<>();
    onCompleteMDC = new Reference<>();
    doAnswer(ignored -> {
      onCompleteClassLoaderRef.set(currentThread().getContextClassLoader());
      onCompleteMDC.set(MDC.getCopyOfContextMap());
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

  @Test
  public void preserveMDCOnError() {
    // Given a non-blocking operation executed with certain MDC.
    Map<String, String> executionMap = singletonMap("on", "execution");
    withMDC(executionMap, () -> executor.execute(executionContext, callback));

    // When the completion callback is completed with error using another MDC.
    Map<String, String> anotherMap = singletonMap("on", "another");
    withMDC(anotherMap, () -> completionCallbackRef.get().error(new NullPointerException()));

    // Then the real on error method is called using the execution MDC.
    assertThat(onErrorMDC.get().get("on"), is("execution"));
  }

  @Test
  public void preserveMDCOnCompletion() {
    // Given a non-blocking operation executed with certain MDC.
    Map<String, String> executionMap = singletonMap("on", "execution");
    withMDC(executionMap, () -> executor.execute(executionContext, callback));

    // When the completion callback is completed using another MDC.
    Map<String, String> anotherMap = singletonMap("on", "another");
    withMDC(anotherMap, () -> completionCallbackRef.get().success(null));

    // Then the real on complete method is called using the execution MDC.
    assertThat(onCompleteMDC.get().get("on"), is("execution"));
  }

  private void withMDC(Map<String, String> mdc, Runnable callback) {
    Map<String, String> oldMDC = MDC.getCopyOfContextMap();
    MDC.setContextMap(mdc);
    try {
      callback.run();
    } finally {
      MDC.setContextMap(oldMDC);
    }
  }
}
