/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.policies;

import static java.time.Duration.ZERO;
import static java.time.Duration.ofMillis;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.retry.reactor.Retry.onlyIf;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.delay;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.core.api.retry.policy.PolicyStatus;
import org.mule.runtime.core.api.retry.policy.RetryPolicy;
import org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate;
import org.mule.runtime.core.internal.retry.reactor.BackoffDelay;
import org.mule.runtime.core.internal.retry.reactor.Retry;
import org.mule.runtime.core.internal.retry.reactor.RetryExhaustedException;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

/**
 * Allows to configure how many times a retry should be attempted and how long to wait between retries.
 */
public class SimpleRetryPolicy implements RetryPolicy {

  protected static final Logger LOGGER = getLogger(SimpleRetryPolicy.class);

  protected RetryCounter retryCounter;

  private volatile int count = SimpleRetryPolicyTemplate.DEFAULT_RETRY_COUNT;
  private volatile Duration frequency = Duration.ofMillis(SimpleRetryPolicyTemplate.DEFAULT_FREQUENCY);

  public SimpleRetryPolicy(long frequency, int retryCount) {
    this.frequency = ofMillis(frequency);
    this.count = retryCount;
    this.retryCounter = new RetryCounter();
  }

  @Override
  public <T> Publisher<T> applyPolicy(Publisher<T> publisher,
                                      Predicate<Throwable> shouldRetry,
                                      Consumer<Throwable> onExhausted,
                                      Function<Throwable, Throwable> errorFunction) {
    return from(publisher)
        .onErrorResume(e -> {
          if (shouldRetry.test(e)) {
            Retry<T> retry = (Retry<T>) onlyIf(ctx -> shouldRetry.test(unwrap(ctx.exception())))
                .backoff(ctx -> new BackoffDelay(frequency, ZERO, ZERO));

            if (count != RETRY_COUNT_FOREVER) {
              retry = retry.retryMax(count - 1);
            }

            Mono<T> retryMono = from(publisher)
                .retryWhen(retry)
                .doOnError(e2 -> onExhausted.accept(unwrap(e2)))
                .onErrorMap(RetryExhaustedException.class, e2 -> errorFunction.apply(unwrap(e2.getCause())));

            if (isTransactionActive()) {
              retryMono = just(retryMono.block());
            }

            return delay(frequency).then(retryMono);

          } else {
            e = unwrap(e);
            onExhausted.accept(e);
            return error(errorFunction.apply(e));
          }
        });
  }

  public PolicyStatus applyPolicy(Throwable cause) {
    if (isExhausted() || !isApplicableTo(cause)) {
      return PolicyStatus.policyExhausted(cause);
    } else {
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(
                    "Waiting for " + frequency.toMillis() + "ms before reconnecting. Failed attempt "
                        + (retryCounter.current().get() + 1)
                        + " of " + (count != RETRY_COUNT_FOREVER ? String.valueOf(count) : "unlimited"));
      }

      try {
        retryCounter.current().getAndIncrement();
        Thread.sleep(frequency.toMillis());
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
    return count != RETRY_COUNT_FOREVER && retryCounter.current().get() >= count;
  }

  protected static class RetryCounter extends ThreadLocal<AtomicInteger> {

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
