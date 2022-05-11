/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.executor;

import static java.util.concurrent.Executors.newFixedThreadPool;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.runtime.streaming.StreamingHelper;
import org.mule.runtime.module.extension.internal.runtime.execution.ArgumentResolverDelegate;
import org.mule.runtime.module.extension.internal.runtime.resolver.ArgumentResolver;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Issue;

public class MethodExecutorGeneratorTestCase extends AbstractMuleContextTestCase {

  private final MethodExecutorGenerator generator = new MethodExecutorGenerator();

  @Before
  public void before() throws InitialisationException {
    generator.setContext(muleContext);
  }

  @Test
  public void sameMethodGeneratesUniqueClass() {
    Method method = getMethod("sampleOperation");
    MethodExecutor executor1 = generator.generate(this, method, mockArgumentResolverDelegate(method));
    MethodExecutor executor2 = generator.generate(this, method, mockArgumentResolverDelegate(method));

    assertThat(executor1, is(notNullValue()));
    assertThat(executor2, is(notNullValue()));
    assertThat(executor1, is(not(sameInstance(executor2))));

    assertThat(executor1.getClass(), is(sameInstance(executor2.getClass())));
  }

  @Test
  @Issue("MULE-19912")
  public void sameMethodGeneratesUniqueClassConcurrently() throws InterruptedException, ExecutionException {
    Method method = getMethod("sampleOperation");

    ExecutorService threadPool = newFixedThreadPool(4);
    try {
      Collection<Future<MethodExecutor>> results = new HashSet<>();
      for (int i = 0; i < 4 * 16; ++i) {
        String ctxId = muleContext.getConfiguration().getId() + i;
        results.add(threadPool
            .submit(() -> {
              // Simulate the case for domains, where the extension is in the domain but this generator exists per app.
              MethodExecutorGenerator generator = new MethodExecutorGenerator();
              generator.setArtifactId(ctxId);
              return generator.generate(this, method, mockArgumentResolverDelegate(method));
            }));
      }

      for (Future<MethodExecutor> result : results) {
        result.get();
      }
    } finally {
      threadPool.shutdown();
    }
  }

  @Test
  public void differentMethodsGenerateDifferentClasses() {
    Method method1 = getMethod("sampleOperation");
    Method method2 = getMethod("anotherOperation");

    MethodExecutor executor1 = generator.generate(this, method1, mockArgumentResolverDelegate(method1));
    MethodExecutor executor2 = generator.generate(this, method2, mockArgumentResolverDelegate(method2));

    assertThat(executor1, is(notNullValue()));
    assertThat(executor2, is(notNullValue()));
    assertThat(executor1, is(not(sameInstance(executor2))));

    assertThat(executor1.getClass(), is(not(sameInstance(executor2.getClass()))));
  }

  private Method getMethod(String methodName) {
    return Stream.of(getClass().getMethods()).filter(m -> m.getName().equals(methodName)).findFirst().get();
  }

  public InputStream sampleOperation(@Config MethodExecutorGeneratorTestCase config,
                                     String param1,
                                     Map<String, Object> map,
                                     StreamingHelper streamingHelper) {
    return new ByteArrayInputStream(param1.getBytes());
  }

  public String anotherOperation(@Config MethodExecutorGeneratorTestCase config, int param1) {
    return "" + param1;
  }

  private ArgumentResolverDelegate mockArgumentResolverDelegate(Method method) {
    ArgumentResolverDelegate delegate = mock(ArgumentResolverDelegate.class);
    ArgumentResolver[] resolvers = new ArgumentResolver[method.getParameterCount()];
    for (int i = 0; i < method.getParameterCount(); i++) {
      resolvers[i] = mock(ArgumentResolver.class);
    }

    when(delegate.getArgumentResolvers()).thenReturn(resolvers);
    return delegate;
  }
}
