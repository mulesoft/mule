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
import reactor.test.subscriber.TestSubscriber;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
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
  private Supplier<CompletableFuture<PolicyStatus>> futureSupplier;
  @Mock
  private Consumer<Throwable> onRetry;

  @BeforeEach
  void setUp() {
    policy = mock(RetryPolicy.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void testApplyPolicy() throws ExecutionException, InterruptedException {
    final CompletableFuture<PolicyStatus> future = policy.applyPolicy(
                                                                      futureSupplier,
                                                                      t -> true,
                                                                      onRetry,
                                                                      onExhausted,
                                                                      errorFunction,
                                                                      retryScheduler);
    final PolicyStatus result = future.get();
    assertThat(result, is(notNullValue()));
  }

  @Test
  void testApplyPolicy1() {
    final Publisher<CoreEvent> result = policy.applyPolicy(publisher, t -> true, onExhausted, errorFunction);
    result.subscribe(TestSubscriber.create());

    assertThat(result, is(notNullValue()));
  }

  @Test
  void testApplyPolicy2() {
    final Publisher<CoreEvent> result = policy.applyPolicy(publiser, t -> true, onExhausted, errorFunction, retryScheduler);
    result.subscribe(TestSubscriber.create());
    assertThat(result, is(notNullValue()));
  }
}
