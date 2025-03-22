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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NoRetryPolicyTestCase {

  private RetryPolicy policy;
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
    policy = new NoRetryPolicyTemplate().createRetryInstance();
  }

  @Test
  void testApplyPolicy() {
    final PolicyStatus result = policy.applyPolicy(new NullPointerException("Todo Pinzón"));

    assertThat(result.isOk(), is(false));
    assertThat(result.getThrowable(), is(instanceOf(NullPointerException.class)));
  }

  @Test
  void testApplyPolicy_exception() {
    final CompletableFuture<PolicyStatus> innerFuture = new CompletableFuture<>();
    NullPointerException exception = new NullPointerException("Perspicacious Pachyderm");

    final CompletableFuture<PolicyStatus> future = policy.applyPolicy(
                                                                      () -> innerFuture,
                                                                      retryPredicate,
                                                                      onRetry,
                                                                      onExhausted,
                                                                      errorFunction,
                                                                      retryScheduler);
    innerFuture.completeExceptionally(exception);

    assertThrows(ExecutionException.class, () -> future.get(100L, TimeUnit.MILLISECONDS));

    verifyNoMoreInteractions(retryPredicate, onRetry, onExhausted, errorFunction, retryScheduler);
  }
}
