/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.policies;

import static java.lang.System.currentTimeMillis;
import static java.util.function.Function.identity;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static reactor.core.Exceptions.unwrap;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.fromCallable;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import org.reactivestreams.Publisher;

public class NonBlockingReconnectionTestCase extends AbstractMuleContextTestCase {

  private static final int RETRIES = 5;
  private static final Long FREQUENCY = 1000L;

  private final AtomicInteger executedRetries = new AtomicInteger(0);
  private final AtomicBoolean exhausted = new AtomicBoolean(false);
  private final AtomicLong previousExecutionMoment = new AtomicLong(0);
  private final List<Long> executionMomentDeltas = new LinkedList<>();

  @Test
  public void successfulWithoutRetry() throws Exception {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(FREQUENCY, RETRIES);
    Publisher<Integer> publisher = fromCallable(() -> {
      trackRetry();

      return executedRetries.get();
    });

    Integer value = from(retryPolicy.applyPolicy(publisher,
                                                 e -> e instanceof IllegalArgumentException,
                                                 e -> exhausted.set(true),
                                                 identity()))
                                                     .block();

    assertThat(value, is(1));
    assertNoRetry();
    assertThat(exhausted.get(), is(false));
  }

  @Test
  public void successfulRetry() throws Exception {
    doSuccessfulRetry(new SimpleRetryPolicy(FREQUENCY, RETRIES));
  }

  @Test
  public void retryForever() throws Exception {
    doSuccessfulRetry(new SimpleRetryPolicy(FREQUENCY, RETRY_COUNT_FOREVER));
  }

  private void doSuccessfulRetry(SimpleRetryPolicy retryPolicy) throws Exception {
    Publisher<Integer> publisher = fromCallable(() -> {
      trackRetry();

      if (executedRetries.get() <= RETRIES) {
        throw new IllegalArgumentException("not retried enough");
      }

      return executedRetries.get();
    });

    Integer value = from(retryPolicy.applyPolicy(publisher,
                                                 e -> e instanceof IllegalArgumentException,
                                                 e -> exhausted.set(true),
                                                 identity()))
                                                     .block();

    assertThat(value, is(RETRIES + 1));
    assertRetry();
    assertThat(exhausted.get(), is(false));
  }



  @Test
  public void exhaustedRetry() {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(FREQUENCY, RETRIES);
    Publisher<Integer> publisher = fromCallable(() -> {
      trackRetry();

      throw new IllegalArgumentException("No retry will save you");
    });

    try {
      from(retryPolicy.applyPolicy(publisher,
                                   e -> e instanceof IllegalArgumentException,
                                   e -> exhausted.set(true),
                                   identity()))
                                       .block();
    } catch (Exception e) {
      assertThat(e, instanceOf(IllegalArgumentException.class));

      assertThat(exhausted.get(), is(true));
      assertRetry();
    }
  }

  @Test
  public void exhaustedRetryWithMappedException() {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(FREQUENCY, RETRIES);
    Publisher<Integer> publisher = fromCallable(() -> {
      trackRetry();

      throw new IllegalArgumentException("No retry will save you");
    });

    try {
      from(retryPolicy.applyPolicy(publisher,
                                   e -> true,
                                   e -> exhausted.set(true),
                                   ConnectionException::new))
                                       .block();
    } catch (Throwable e) {
      e = unwrap(e);
      assertThat(e, instanceOf(ConnectionException.class));
      assertThat(e.getCause(), instanceOf(IllegalArgumentException.class));

      assertThat(exhausted.get(), is(true));
      assertRetry();
    }
  }

  @Test
  public void skipRetry() throws Exception {
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(FREQUENCY, RETRIES);
    Publisher<Integer> publisher = fromCallable(() -> {
      trackRetry();

      throw new IllegalArgumentException("No retry will save you");
    });

    try {
      from(retryPolicy.applyPolicy(publisher,
                                   e -> !(e instanceof IllegalArgumentException),
                                   e -> exhausted.set(true),
                                   identity()))
                                       .block();
    } catch (Exception e) {
      assertThat(e, instanceOf(IllegalArgumentException.class));

      assertNoRetry();
      assertThat(exhausted.get(), is(true));
    }
  }

  private void assertNoRetry() {
    assertThat(executedRetries.get(), is(1));
    assertThat(executionMomentDeltas, hasSize(0));
  }

  private void assertRetry() {
    assertThat(executedRetries.get(), is(RETRIES + 1));
    assertThat(executionMomentDeltas.stream().mapToInt(Long::intValue).average().getAsDouble(),
               anyOf(greaterThanOrEqualTo(FREQUENCY.doubleValue()), lessThanOrEqualTo(FREQUENCY.doubleValue() * 1.3)));
  }

  private void trackRetry() {
    executedRetries.incrementAndGet();
    long now = currentTimeMillis();
    if (!previousExecutionMoment.compareAndSet(0, now)) {
      final long delta = now - previousExecutionMoment.get();
      previousExecutionMoment.set(now);
      executionMomentDeltas.add(delta);
    }
  }
}
