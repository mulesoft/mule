/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.retry.policies;

import org.mule.runtime.core.api.retry.RetryPolicy;
import org.mule.runtime.core.retry.PolicyStatus;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
  public void applyOn(Mono<?> publisher, Predicate<Throwable> predicate) {
    publisher.retry(count, e -> predicate.test(e) && applyPolicy(e).isOk());
  }

  @Override
  public void applyOn(Flux<?> publisher, Predicate<Throwable> predicate) {
    publisher.retry(count, e -> predicate.test(e) && applyPolicy(e).isOk());
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
