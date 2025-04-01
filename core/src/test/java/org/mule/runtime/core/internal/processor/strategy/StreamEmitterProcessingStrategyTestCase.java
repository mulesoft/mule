/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.processor.strategy.StreamEmitterProcessingStrategyFactory.StreamEmitterProcessingStrategy;
import org.reactivestreams.Publisher;
import reactor.test.publisher.TestPublisher;
import reactor.test.subscriber.TestSubscriber;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class StreamEmitterProcessingStrategyTestCase {

  private static ExecutorService service;
  private StreamEmitterProcessingStrategyFactory.StreamEmitterProcessingStrategy strategy;
  private AtomicLong timeout;
  @Mock
  private Scheduler flowScheduler;
  @Mock
  private Scheduler cpuLiteScheduler;
  @Mock
  private CoreEvent event;

  @BeforeAll
  static void setupExecutor() {
    service = Executors.newSingleThreadExecutor();
  }

  @AfterAll
  static void teardownExecutor() {
    service.shutdown();
  }


  @BeforeEach
  void setUp() {
    timeout = new AtomicLong(0L);
    strategy = new StreamEmitterProcessingStrategy(
                                                   2,
                                                   1,
                                                   () -> flowScheduler,
                                                   () -> cpuLiteScheduler,
                                                   2,
                                                   2,
                                                   true,
                                                   () -> timeout.get());
  }

  @Test
  void dispose_timeOut() throws InterruptedException, ExecutionException, TimeoutException {
    timeout.set(-10);
    TestPublisher<CoreEvent> testPublisher = TestPublisher.create();
    final Publisher<CoreEvent> pub = strategy.configureInternalPublisher(testPublisher);
    final Future<?> future = disposeInFuture(10);
    pub.subscribe(TestSubscriber.create());
    testPublisher.complete(); // Trigger the decrement active sinks.
    future.get(100L, TimeUnit.MILLISECONDS);
  }

  @Test
  void dispose_waitForTimeOut() throws InterruptedException, ExecutionException, TimeoutException {
    timeout.set(100);
    TestPublisher<CoreEvent> testPublisher = TestPublisher.create();
    final Publisher<CoreEvent> pub = strategy.configureInternalPublisher(testPublisher);
    pub.subscribe(TestSubscriber.create());
    final Future<?> future = disposeInFuture(10);
    Thread.sleep(50L); // Have to wait so the dispose loop that sleeps for 10 millis has a chance to loop...
    testPublisher.complete(); // Trigger the decrement active sinks.
    future.get(100L, TimeUnit.MILLISECONDS);
  }

  @Test
  void dispose_waitInterrupted() throws InterruptedException, ExecutionException, TimeoutException {
    timeout.set(100);
    TestPublisher<CoreEvent> testPublisher = TestPublisher.create();
    final Publisher<CoreEvent> pub = strategy.configureInternalPublisher(testPublisher);
    pub.subscribe(TestSubscriber.create());
    final Future<?> future = disposeInFuture(10);
    Thread.sleep(20L); // Have to wait so the dispose loop that sleeps for 10 millis has a chance to loop...
    future.cancel(true);
    Thread.sleep(20L);
    assertThrows(CancellationException.class, () -> future.get(100L, TimeUnit.MILLISECONDS));
  }

  @Test
  void checkCapacity_withRetrySet() {
    strategy.onRejected(flowScheduler); // Set up that we want to retry

    BackPressureReason backPressureReason = strategy.checkBackpressureEmitting(event);
    assertThat(backPressureReason, is(BackPressureReason.REQUIRED_SCHEDULER_BUSY));
  }

  private Future<?> disposeInFuture(int timeout) throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);

    final Future<?> result = service.submit(() -> {
      latch.countDown();
      strategy.dispose();
    });
    latch.await(timeout, TimeUnit.MILLISECONDS);
    return result;
  }
}
