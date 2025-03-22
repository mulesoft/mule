/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

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
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.management.stats.AllStatistics;
import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.internal.processor.strategy.MockInjector;
import org.mule.runtime.core.internal.processor.strategy.reactor.builder.ParameterMockingTestUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.config.MuleRuntimeFeature.COMPUTE_CONNECTION_ERRORS_IN_STATS;
import static org.mule.runtime.api.notification.ConnectionNotification.CONNECTION_CONNECTED;
import static org.mule.runtime.api.notification.ConnectionNotification.CONNECTION_FAILED;

@ExtendWith(MockitoExtension.class)
class AbstrctPolicyTemplateTestCase {

  private AbstractPolicyTemplate template;
  @Mock
  private NotificationDispatcher dispatcher;
  @Mock
  private MuleContext context;
  @Mock
  private FeatureFlaggingService flaggingService;
  @Mock
  private RetryCallback callback;
  @Mock
  private Executor workManager;
  @Mock
  private RetryPolicy policy;
  @Captor
  private ArgumentCaptor<Notification> notificationCaptor;
  @Mock
  private AllStatistics statistics;
  @Mock
  private FlowConstructStatistics appStatistics;

  @BeforeEach
  void setUp() {
    template = mock(AbstractPolicyTemplate.class, withSettings().useConstructor().defaultAnswer(CALLS_REAL_METHODS));
    MockInjector.injectMocksFromSuite(this, template);
  }

  @Test
  void execute() throws Exception {
    when(template.createRetryInstance()).thenReturn(policy);

    final RetryContext result = template.execute(callback, workManager);

    assertThat(result, is(notNullValue()));
    verify(dispatcher).dispatch(notificationCaptor.capture());
    assertThat(notificationCaptor.getValue().getAction().getIdentifier(), is(String.valueOf(CONNECTION_CONNECTED)));
  }

  @Test
  void execute_withError() throws Exception {
    when(template.createRetryInstance()).thenReturn(policy);
    NullPointerException exception = new NullPointerException("binky!");
    when(policy.applyPolicy(any())).thenReturn(PolicyStatus.policyOk(), PolicyStatus.policyExhausted(exception));
    doThrow(exception).when(callback).doWork(any());

    assertThrows(RetryPolicyExhaustedException.class, () -> template.execute(callback, workManager));

    verify(dispatcher, times(2)).dispatch(notificationCaptor.capture());
    assertThat(notificationCaptor.getAllValues().get(0).getAction().getIdentifier(), is(String.valueOf(CONNECTION_FAILED)));
    assertThat(notificationCaptor.getAllValues().get(1).getAction().getIdentifier(), is(String.valueOf(CONNECTION_FAILED)));
  }

  @Test
  void execute_interrupted() throws Exception {
    when(template.createRetryInstance()).thenReturn(policy);
    InterruptedException exception = new InterruptedException("binky!");
    doThrow(exception).when(callback).doWork(any());

    final RetryContext result = template.execute(callback, workManager);

    assertThat(result, is(notNullValue()));
    verify(dispatcher).dispatch(notificationCaptor.capture());
    assertThat(notificationCaptor.getValue().getAction().getIdentifier(), is(String.valueOf(CONNECTION_FAILED)));
  }

  @Test
  void execute_errorStats() throws Exception {
    InterruptedException exception = new InterruptedException("binky!");
    doThrow(exception).when(callback).doWork(any());
    when(context.getStatistics()).thenReturn(statistics);
    when(statistics.isEnabled()).thenReturn(true);
    when(statistics.getApplicationStatistics()).thenReturn(appStatistics);
    when(flaggingService.isEnabled(any())).thenReturn(true);

    final RetryContext result = template.execute(callback, workManager);

    assertThat(result, is(notNullValue()));
    verify(appStatistics).incConnectionErrors();
    verify(flaggingService).isEnabled(COMPUTE_CONNECTION_ERRORS_IN_STATS);
  }


  @ParameterizedTest(name = "[{index}]{0}")
  @MethodSource("fluxSinkMethods")
  void fluxSinkDelegates(String testName, Method getter, Method setter) throws InvocationTargetException, IllegalAccessException {
    Object[] values = ParameterMockingTestUtil.getParameterValues(setter, CoreEvent.class);
    setter.invoke(template, values);
    assertThat(getter.invoke(template), is(values[0]));
  }

  static List<Arguments> fluxSinkMethods() {
    return Arrays.stream(RetryPolicyTemplate.class.getDeclaredMethods())
        .filter(m -> m.getName().matches("^(get|set).*"))
        .collect(Collectors.groupingBy(m -> m.getName().substring(3))).values().stream()
        .filter(methods -> methods.size() == 2)
        .map(methods -> methods.stream().sorted(Comparator.comparing(Method::getName)).toList())
        .map(methods -> Arguments.of(methods.get(0).getName(), methods.get(0), methods.get(1)))
        .toList();
  }
}

