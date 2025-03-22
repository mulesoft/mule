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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.util.rx.ImmediateScheduler;
import org.reactivestreams.Publisher;
import reactor.test.publisher.TestPublisher;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class RetryPolicyTemplateTest {

  private RetryPolicyTemplate template;
  private TestPublisher<CoreEvent> publisher;
  private Exception exception;
  @Mock
  private Consumer<Throwable> onExhausted;
  @Mock
  private Function<Throwable, Throwable> errorFunction;
  @Mock
  private Scheduler retryScheduler;
  @Mock
  private Consumer<Throwable> onRetry;
  @Mock
  private Predicate<Throwable> shouldRetry;
  @Mock
  private RetryPolicy policy;
  @Captor
  private ArgumentCaptor<Predicate<Throwable>> shouldRetryCaptor;
  @Captor
  private ArgumentCaptor<Function<Throwable, Throwable>> errorFunctionCaptor;
  @Captor
  private ArgumentCaptor<Scheduler> retrySchedulerCaptor;
  @Captor
  private ArgumentCaptor<Consumer<Throwable>> onRetryCaptor;

  @BeforeEach
  void setup() {
    publisher = TestPublisher.create();
    exception = new IllegalStateException("No avaliable meerkats");
    template = mock(RetryPolicyTemplate.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void applyPolicy() {
    when(template.createRetryInstance()).thenReturn(policy);
    template.applyPolicy(publisher);

    verify(policy).applyPolicy(eq(publisher), shouldRetryCaptor.capture(), any(), errorFunctionCaptor.capture(),
                               retrySchedulerCaptor.capture());
    assertThat(shouldRetryCaptor.getValue().test(exception), is(true));
    assertThat(errorFunctionCaptor.getValue().apply(exception), is(exception));
    assertThat(retrySchedulerCaptor.getValue(), is(instanceOf(ImmediateScheduler.class)));
  }

  @Test
  void applyPolicyWithScheduler() {
    when(template.createRetryInstance()).thenReturn(policy);
    template.applyPolicy(publisher, retryScheduler);

    verify(policy).applyPolicy(eq(publisher), shouldRetryCaptor.capture(), any(), errorFunctionCaptor.capture(),
                               eq(retryScheduler));
    assertThat(shouldRetryCaptor.getValue().test(exception), is(true));
    assertThat(errorFunctionCaptor.getValue().apply(exception), is(exception));
  }

  @Test
  void isEnabled() {
    assertThat(template.isEnabled(), is(true));
  }

  @Test
  void testApplyPolicy() {
    when(template.createRetryInstance()).thenReturn(policy);
    template.applyPolicy(publisher, shouldRetry, onExhausted, errorFunction, retryScheduler);

    verify(policy).applyPolicy(eq(publisher), eq(shouldRetry), eq(onExhausted), eq(errorFunction), eq(retryScheduler));
  }

  @Test
  void testApplyPolicy1() {
    when(template.createRetryInstance()).thenReturn(policy);
    template.applyPolicy(CompletableFuture::new, shouldRetry, onRetry, onExhausted, errorFunction, retryScheduler);

    verify(policy).applyPolicy(any(), eq(shouldRetry), eq(onRetry), eq(onExhausted), eq(errorFunction), eq(retryScheduler));
  }

  @Test
  void isAsync() {
    assertThat(template.isAsync(), is(false));
  }
}
