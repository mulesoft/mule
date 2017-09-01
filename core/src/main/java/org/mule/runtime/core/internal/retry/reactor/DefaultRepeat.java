/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;
import reactor.util.Loggers;

public class DefaultRepeat<T> extends AbstractRetry<T, Long> implements Repeat<T> {

  static final Logger log = Loggers.getLogger(DefaultRepeat.class);
  static final Consumer<? super RepeatContext<?>> NOOP_ON_REPEAT = r -> {
  };

  final Predicate<? super RepeatContext<T>> repeatPredicate;
  final Consumer<? super RepeatContext<T>> onRepeat;

  DefaultRepeat(Predicate<? super RepeatContext<T>> repeatPredicate,
                int maxRepeats,
                Duration timeout,
                Backoff backoff,
                Jitter jitter,
                Scheduler backoffScheduler,
                final Consumer<? super RepeatContext<T>> onRepeat,
                T applicationContext) {
    super(maxRepeats, timeout, backoff, jitter, backoffScheduler, applicationContext);
    this.repeatPredicate = repeatPredicate;
    this.onRepeat = onRepeat;
  }

  public static <T> DefaultRepeat<T> create(Predicate<? super RepeatContext<T>> repeatPredicate, int n) {
    return new DefaultRepeat<T>(repeatPredicate,
                                n,
                                null,
                                Backoff.zero(),
                                Jitter.noJitter(),
                                null,
                                NOOP_ON_REPEAT,
                                (T) null);
  }

  @Override
  public Repeat<T> withApplicationContext(T applicationContext) {
    return new DefaultRepeat<>(repeatPredicate, maxIterations, timeout,
                               backoff, jitter, backoffScheduler, onRepeat, applicationContext);
  }

  @Override
  public Repeat<T> doOnRepeat(Consumer<? super RepeatContext<T>> onRepeat) {
    return new DefaultRepeat<>(repeatPredicate, maxIterations, timeout,
                               backoff, jitter, backoffScheduler, onRepeat, applicationContext);
  }

  @Override
  public Repeat<T> timeout(Duration timeout) {
    if (timeout.isNegative())
      throw new IllegalArgumentException("timeout should be >= 0");
    return new DefaultRepeat<>(repeatPredicate, Integer.MAX_VALUE, timeout,
                               backoff, jitter, backoffScheduler, onRepeat, applicationContext);
  }

  @Override
  public Repeat<T> backoff(Backoff backoff) {
    return new DefaultRepeat<>(repeatPredicate, maxIterations, timeout,
                               backoff, jitter, backoffScheduler, onRepeat, applicationContext);
  }

  @Override
  public Repeat<T> jitter(Jitter jitter) {
    return new DefaultRepeat<>(repeatPredicate, maxIterations, timeout,
                               backoff, jitter, backoffScheduler, onRepeat, applicationContext);
  }

  @Override
  public Repeat<T> withBackoffScheduler(Scheduler scheduler) {
    return new DefaultRepeat<>(repeatPredicate, maxIterations, timeout,
                               backoff, jitter, scheduler, onRepeat, applicationContext);
  }

  @Override
  public Publisher<Long> apply(Flux<Long> companionValues) {
    Instant timeoutInstant = calculateTimeout();
    DefaultContext<T> context = new DefaultContext<>(applicationContext, 0, null, -1L);
    return companionValues
        .zipWith(Flux.range(1, Integer.MAX_VALUE), (c, i) -> repeatBackoff(c, i, timeoutInstant, context))
        .takeWhile(backoff -> backoff != RETRY_EXHAUSTED)
        .concatMap(backoff -> retryMono(backoff.delay));
  }

  BackoffDelay repeatBackoff(Long companionValue, Integer iteration, Instant timeoutInstant, DefaultContext<T> context) {
    DefaultContext<T> tmpContext = new DefaultContext<>(applicationContext, iteration, context.lastBackoff, companionValue);
    BackoffDelay nextBackoff = calculateBackoff(tmpContext, timeoutInstant);
    DefaultContext<T> repeatContext = new DefaultContext<>(applicationContext, iteration, nextBackoff, companionValue);
    context.lastBackoff = nextBackoff;

    if (!repeatPredicate.test(repeatContext)) {
      log.debug("Stopping repeats since predicate returned false, retry context: {}", repeatContext);
      return RETRY_EXHAUSTED;
    } else if (nextBackoff == RETRY_EXHAUSTED) {
      log.debug("Repeats exhausted, retry context: {}", repeatContext);
      return RETRY_EXHAUSTED;
    } else {
      log.debug("Scheduling repeat attempt, retry context: {}", repeatContext);
      onRepeat.accept(repeatContext);
      return nextBackoff;
    }
  }
}

