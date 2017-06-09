/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.policies;

import static java.time.Duration.ofMillis;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.range;
import static reactor.core.publisher.Mono.delay;
import static reactor.core.publisher.Mono.error;

import org.mule.runtime.core.api.retry.policy.RetryPolicy;
import org.mule.runtime.core.api.retry.policy.PolicyStatus;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

/**
 * Allows to configure how many times a retry should be attempted and how long to wait between retries.
 */
public class SimpleRetryPolicy implements RetryPolicy {

  protected static final Logger logger = LoggerFactory.getLogger(SimpleRetryPolicy.class);

  protected RetryCounter retryCounter;

  private volatile int count = SimpleRetryPolicyTemplate.DEFAULT_RETRY_COUNT;
  private volatile long frequency = SimpleRetryPolicyTemplate.DEFAULT_FREQUENCY;

  public SimpleRetryPolicy(long frequency, int retryCount) {
    this.frequency = frequency;
    this.count = retryCount;
    retryCounter = new RetryCounter();
  }

  @Override
  public <T> Publisher<T> applyPolicy(Publisher<T> publisher,
                                      Predicate<Throwable> shouldRetry,
                                      Consumer<Throwable> onExhausted) {
    final int actualCount = count + 1;
    return from(publisher).retryWhen(errors -> errors.zipWith(range(1, actualCount), Tuples::of)
        .flatMap(tuple -> {
          final Throwable exception = tuple.getT1();
          if (tuple.getT2() == actualCount || !shouldRetry.test(exception)) {
            onExhausted.accept(exception);
            return (Mono<T>) error(exception);
          } else {
            return delay(ofMillis(frequency));
          }
        }));
  }

  public PolicyStatus applyPolicy(Throwable cause) {

    if (isExhausted() || !isApplicableTo(cause)) {
      return PolicyStatus.policyExhausted(cause);
    } else {
      if (logger.isInfoEnabled()) {
        logger.info("Waiting for " + frequency + "ms before reconnecting. Failed attempt " + (retryCounter.current().get() + 1)
            + " of " + (count != SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER ? String.valueOf(count) : "unlimited"));
      }

      try {
        retryCounter.current().getAndIncrement();
        Thread.sleep(frequency);
        return PolicyStatus.policyOk();
      } catch (InterruptedException e) {
        // If we get an interrupt exception, some one is telling us to stop
        return PolicyStatus.policyExhausted(e);
      }
    }
  }

  /**
   * Indicates if the policy is applicable for the cause that caused the policy invocation. Subclasses can override this method in
   * order to filter the type of exceptions that does not deserve a retry.
   *
   * @return true if the policy is applicable, false otherwise.
   */
  protected boolean isApplicableTo(Throwable cause) {
    return true;
  }

  /**
   * Determines if the policy is exhausted or not comparing the original configuration against the current state.
   */
  protected boolean isExhausted() {
    return count != SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER && retryCounter.current().get() >= count;
  }

  protected static class RetryCounter extends ThreadLocal<AtomicInteger> {

    public int countRetry() {
      return get().incrementAndGet();
    }

    public void reset() {
      get().set(0);
    }

    public AtomicInteger current() {
      return get();
    }

    @Override
    protected AtomicInteger initialValue() {
      return new AtomicInteger(0);
    }
  }
}
