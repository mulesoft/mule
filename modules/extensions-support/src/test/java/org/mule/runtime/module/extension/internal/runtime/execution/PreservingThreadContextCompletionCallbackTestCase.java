/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import static java.lang.Thread.currentThread;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.mockito.Mock;
import org.slf4j.MDC;

@Issue("MULE-19097")
public class PreservingThreadContextCompletionCallbackTestCase extends AbstractMuleTestCase {

  @Mock
  private CompletionCallback<Object, Object> completionCallback = mock(CompletionCallback.class);

  @Test
  public void preserveClassLoaderOnError() {
    Reference<PreservingThreadContextCompletionCallback> preservingCtxExecutorCallbackRef = new Reference<>();

    ClassLoader onCreationClassLoader = mock(ClassLoader.class);
    withContextClassLoader(onCreationClassLoader, () -> {
      preservingCtxExecutorCallbackRef.set(new PreservingThreadContextCompletionCallback<>(completionCallback));
    });

    Reference<ClassLoader> onErrorClassLoaderRef = new Reference<>();
    doAnswer(ignored -> {
      onErrorClassLoaderRef.set(currentThread().getContextClassLoader());
      return null;
    }).when(completionCallback).error(any());

    ClassLoader anotherClassLoader = mock(ClassLoader.class);
    withContextClassLoader(anotherClassLoader, () -> {
      preservingCtxExecutorCallbackRef.get().error(new NullPointerException());
    });

    assertThat(onErrorClassLoaderRef.get(), is(onCreationClassLoader));
  }

  @Test
  public void preserveClassLoaderOnComplete() {
    Reference<PreservingThreadContextCompletionCallback> preservingCtxExecutorCallbackRef = new Reference<>();

    ClassLoader onCreationClassLoader = mock(ClassLoader.class);
    withContextClassLoader(onCreationClassLoader, () -> {
      preservingCtxExecutorCallbackRef.set(new PreservingThreadContextCompletionCallback<>(completionCallback));
    });

    Reference<ClassLoader> onCompleteClassLoaderRef = new Reference<>();
    doAnswer(ignored -> {
      onCompleteClassLoaderRef.set(currentThread().getContextClassLoader());
      return null;
    }).when(completionCallback).success(any());

    ClassLoader anotherClassLoader = mock(ClassLoader.class);
    withContextClassLoader(anotherClassLoader, () -> {
      preservingCtxExecutorCallbackRef.get().success(null);
    });

    assertThat(onCompleteClassLoaderRef.get(), is(onCreationClassLoader));
  }

  @Test
  public void preserveMDCOnError() {
    Reference<PreservingThreadContextCompletionCallback> preservingCtxExecutorCallbackRef = new Reference<>();

    Map<String, String> onCreationMap = singletonMap("on", "creation");
    withMDC(onCreationMap, () -> {
      preservingCtxExecutorCallbackRef.set(new PreservingThreadContextCompletionCallback<>(completionCallback));
    });

    Reference<Map<String, String>> onErrorMDCRef = new Reference<>();
    doAnswer(ignored -> {
      onErrorMDCRef.set(MDC.getCopyOfContextMap());
      return null;
    }).when(completionCallback).error(any());

    Map<String, String> anotherMap = singletonMap("on", "another");
    withMDC(anotherMap, () -> {
      preservingCtxExecutorCallbackRef.get().error(new NullPointerException());
    });

    assertThat(onErrorMDCRef.get().get("on"), is("creation"));
  }

  @Test
  public void preserveMDCOnComplete() {
    Reference<PreservingThreadContextCompletionCallback> preservingCtxExecutorCallbackRef = new Reference<>();

    Map<String, String> onCreationMap = singletonMap("on", "creation");
    withMDC(onCreationMap, () -> {
      preservingCtxExecutorCallbackRef.set(new PreservingThreadContextCompletionCallback<>(completionCallback));
    });

    Reference<Map<String, String>> onCompleteMDCRef = new Reference<>();
    doAnswer(ignored -> {
      onCompleteMDCRef.set(MDC.getCopyOfContextMap());
      return null;
    }).when(completionCallback).success(any());

    Map<String, String> anotherMap = singletonMap("on", "another");
    withMDC(anotherMap, () -> {
      preservingCtxExecutorCallbackRef.get().success(null);
    });

    assertThat(onCompleteMDCRef.get().get("on"), is("creation"));
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
