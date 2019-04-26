/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.policies;

import static java.time.Duration.ofMillis;
import static org.mule.runtime.core.api.retry.policy.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.delay;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.just;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import static reactor.retry.Retry.onlyIf;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.retry.policy.PolicyStatus;
import org.mule.runtime.core.api.retry.policy.RetryPolicy;
import org.mule.runtime.core.internal.util.rx.ConditionalExecutorServiceDecorator;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.jodah.failsafe.Failsafe;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;
import reactor.retry.BackoffDelay;
import reactor.retry.Retry;
import reactor.retry.RetryExhaustedException;

/**
 * Allows to configure how many times a retry should be attempted and how long to wait between retries.
 */
public class SimpleRetryPolicy implements RetryPolicy {

  private static final Logger LOGGER = getLogger(SimpleRetryPolicy.class);

  protected RetryCounter retryCounter;

  private final int count;
  private final Duration frequency;

  public SimpleRetryPolicy(long frequency, int retryCount) {
    this.frequency = ofMillis(frequency);
    this.count = retryCount;
    this.retryCounter = new RetryCounter();
  }

  @Override
  public <T> CompletableFuture<T> applyPolicy(Supplier<CompletableFuture<T>> futureSupplier,
                                              Predicate<Throwable> shouldRetry,
                                              Consumer<Throwable> onRetry,
                                              Consumer<Throwable> onExhausted,
                                              Function<Throwable, Throwable> errorFunction,
                                              Scheduler retryScheduler) {

    net.jodah.failsafe.RetryPolicy<Object> actingPolicy = new net.jodah.failsafe.RetryPolicy<>()
        .handleIf(shouldRetry)
        .withMaxRetries(count != RETRY_COUNT_FOREVER ? count : -1)
        .withDelay(frequency)
        .onRetry(listener -> onRetry.accept(listener.getLastFailure()))
        .onRetriesExceeded(listener -> {
          LOGGER.info("Retry attempts exhausted. Failing...");
          Throwable t = errorFunction.apply(listener.getFailure());
          onExhausted.accept(t);
        });

    final IsFirst first = new IsFirst();
    final LazyValue<Boolean> isTransanctional = new LazyValue<>(() -> isTransactionActive());

    return Failsafe.with(actingPolicy)
        .with(new ConditionalExecutorServiceDecorator(retryScheduler, s -> first.isFirst() || isTransanctional.get()))
        .getStageAsync(futureSupplier::get);
  }

  private class IsFirst {

    private boolean first = true;

    public boolean isFirst() {
      if (first) {
        first = false;
        return true;
      }

      return false;
    }
  }


  @Override
  public <T> Publisher<T> applyPolicy(Publisher<T> publisher,
                                      Predicate<Throwable> shouldRetry,
                                      Consumer<Throwable> onExhausted,
                                      Function<Throwable, Throwable> errorFunction,
                                      Scheduler retryScheduler) {
    return from(publisher).onErrorResume(e -> {
      if (shouldRetry.test(e)) {
        Retry<T> retry = (Retry<T>) onlyIf(ctx -> shouldRetry.test(unwrap(ctx.exception())))
            .backoff(ctx -> new BackoffDelay(frequency));

        if (count != RETRY_COUNT_FOREVER) {
          retry = retry.retryMax(count - 1);
        }

        final LazyValue<Boolean> isTransanctional = new LazyValue<>(() -> isTransactionActive());
        reactor.core.scheduler.Scheduler reactorRetryScheduler =
            fromExecutorService(new ConditionalExecutorServiceDecorator(retryScheduler, s -> isTransanctional.get()));

        Mono<T> retryMono = from(publisher)
            .retryWhen(retry.withBackoffScheduler(reactorRetryScheduler)
                .doOnRetry(retryContext -> LOGGER.info("Retrying execution of event, attempt {} of {}.", retryContext.iteration(),
                                                       count != RETRY_COUNT_FOREVER ? String.valueOf(count) : "unlimited")))
            .doOnError(e2 -> {
              LOGGER.info("Retry attempts exhausted. Failing...");
              onExhausted.accept(unwrap(e2));
            })
            .onErrorMap(RetryExhaustedException.class, e2 -> errorFunction.apply(unwrap(e2.getCause())));
        return delay(frequency, reactorRetryScheduler).then(isTransactionActive() ? just(retryMono.block()) : retryMono);
      } else {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Not retrying execution of event. Failing...");
        }
        e = unwrap(e);
        onExhausted.accept(e);
        return error(errorFunction.apply(e));
      }
    });
  }

  @Override
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
