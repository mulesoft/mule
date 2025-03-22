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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.reactivestreams.Publisher;
import reactor.test.publisher.TestPublisher;
import reactor.test.subscriber.TestSubscriber;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class RetryPolicyTestCase {

  private RetryPolicy policy;
  @Mock
  private Publisher<CoreEvent> publisher;
  @Mock
  private Consumer<Throwable> onExhausted;
  @Mock
  private Function<Throwable, Throwable> errorFunction;
  @Mock
  private Scheduler retryScheduler;
  @Mock
  private Consumer<Throwable> onRetry;
  @Mock
  private Predicate<Throwable> retryPredicate;

  @BeforeEach
  void setUp() {
    policy = mock(RetryPolicy.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void testApplyPolicy_exception() {
    final CompletableFuture<PolicyStatus> innerFuture = new CompletableFuture<>();
    NullPointerException exception = new NullPointerException("Perspicacious Pachyderm");
    when(errorFunction.apply(any())).thenAnswer(inv -> inv.getArgument(0));

    final CompletableFuture<PolicyStatus> future = policy.applyPolicy(
                                                                      () -> innerFuture,
                                                                      retryPredicate,
                                                                      onRetry,
                                                                      onExhausted,
                                                                      errorFunction,
                                                                      retryScheduler);
    innerFuture.completeExceptionally(exception);

    assertThrows(ExecutionException.class, () -> future.get(100L, TimeUnit.MILLISECONDS));

    verify(errorFunction).apply(exception);
    verify(onExhausted).accept(exception);
    verifyNoMoreInteractions(retryPredicate, onRetry, onExhausted, errorFunction, retryScheduler);
  }

  @Test
  void testApplyPolicy_complete() throws ExecutionException, InterruptedException, TimeoutException {
    final CompletableFuture<PolicyStatus> innerFuture = new CompletableFuture<>();

    final CompletableFuture<PolicyStatus> future = policy.applyPolicy(
                                                                      () -> innerFuture,
                                                                      retryPredicate,
                                                                      onRetry,
                                                                      onExhausted,
                                                                      errorFunction,
                                                                      retryScheduler);
    innerFuture.complete(PolicyStatus.policyOk());

    final PolicyStatus result = future.get(100L, TimeUnit.MILLISECONDS);

    assertThat(result.isOk(), is(true));
    verifyNoMoreInteractions(retryPredicate, onRetry, onExhausted, errorFunction, retryScheduler);
  }

  @Test
  void testApplyPolicy1() {
    final Publisher<CoreEvent> result = policy.applyPolicy(publisher, retryPredicate, onExhausted, errorFunction);
    result.subscribe(TestSubscriber.create());

    assertThat(result, is(notNullValue()));
    verifyNoMoreInteractions(retryPredicate, onRetry, onExhausted, errorFunction, retryScheduler);
  }

  @Test
  void testApplyPolicy2() {
    TestPublisher<CoreEvent> testPublisher = TestPublisher.create();
    final Publisher<CoreEvent> result =
        policy.applyPolicy(testPublisher, retryPredicate, onExhausted, errorFunction, retryScheduler);
    result.subscribe(TestSubscriber.create());
    testPublisher.next(CoreEvent.nullEvent());
    testPublisher.error(new IllegalArgumentException("Too many finches!"));
    testPublisher.complete();

    assertThat(result, is(notNullValue()));
    verifyNoMoreInteractions(retryPredicate, onRetry, onExhausted, errorFunction, retryScheduler);
  }
}
