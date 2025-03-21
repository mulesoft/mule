/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.async;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.internal.processor.strategy.MockInjector;
import org.mule.runtime.core.internal.processor.strategy.reactor.builder.ParameterMockingTestUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsynchronousRetryTemplateTestCase {

  private AsynchronousRetryTemplate template;
  @Mock(extraInterfaces = {Initialisable.class, Startable.class, Stoppable.class, Disposable.class})
  private RetryPolicyTemplate delegate;
  @Mock
  private RetryCallback callback;
  @Mock
  private Executor workManager;
  @Captor
  private ArgumentCaptor<Runnable> commandCaptor;
  @Mock
  private RetryContext retryContext;
  @Mock
  private MuleContext context;
  @Mock
  private Injector injector;

  @BeforeEach
  void setUp() {
    template = new AsynchronousRetryTemplate(delegate);
  }

  @Test
  void execute() throws Exception {
    when(delegate.execute(any(), any())).thenReturn(retryContext);
    when(retryContext.isOk()).thenReturn(true);
    final RetryContext result = template.execute(callback, workManager);

    assertThat(result, is(notNullValue()));
    verify(workManager).execute(commandCaptor.capture());
    commandCaptor.getValue().run();
    assertThat(result.isOk(), is(true));
    verify(retryContext).isOk();
  }

  @ParameterizedTest(name = "[{index}]{0}")
  @MethodSource("delegationMethods")
  void checkDelegation(String testName, Method m) throws InvocationTargetException, IllegalAccessException {
    Object[] values = ParameterMockingTestUtil.getParameterValues(m, CoreEvent.class);
    m.invoke(template, values);
    m.invoke(verify(delegate), values);
  }

  @ParameterizedTest(name = "[{index}]{0}")
  @MethodSource("lifecycleMethods")
  void checkLifecycle_implemented(String testName, Method m)
      throws InvocationTargetException, IllegalAccessException, MuleException {
    MockInjector.injectMocksFromSuite(this, template);
    lenient().when(context.getInjector()).thenReturn(injector);
    lenient().when(injector.inject(any())).thenAnswer(inv -> {
      final Object o = inv.getArgument(0);
      MockInjector.injectMocksFromSuite(this, o);
      return o;
    });
    m.invoke(template);
    m.invoke(verify(delegate));
  }

  static List<Arguments> delegationMethods() {
    return Arrays.stream(RetryPolicyTemplate.class.getDeclaredMethods())
        .filter(m -> m.getName().matches("^[a-z].*"))
        .filter(m -> !(m.getName().equals("execute") || m.getName().equals("isAsync")))
        .filter(m -> !m.getName().equals("applyPolicy"))
        .filter(m -> !Modifier.isStatic(m.getModifiers()))
        .map(m -> Arguments.of(m.getName(), m))
        .toList();
  }

  static List<Arguments> lifecycleMethods() {
    return List.of(Initialisable.class, Startable.class, Stoppable.class, Disposable.class).stream()
        .flatMap(c -> Arrays.stream(c.getDeclaredMethods()))
        .filter(m -> m.getName().matches("^[a-z].*"))
        .filter(m -> !Modifier.isStatic(m.getModifiers()))
        .map(m -> Arguments.of(m.getName(), m))
        .toList();
  }
}
