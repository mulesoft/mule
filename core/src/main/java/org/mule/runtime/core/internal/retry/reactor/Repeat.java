/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

/**
 * Repeat function that may be used with {@link Flux#repeatWhen(Function)},
 * {@link Mono#repeatWhen(Function)} and {@link Mono#repeatWhenEmpty(Function)}.
 * <p>
 * Example usage:
 * <pre><code>
 *   repeat = Repeat.times(10)
 *                  .randomBackoff(Duration.ofMillis(100), Duration.ofSeconds(60))
 *                  .withApplicationContext(appContext)
 *                  .doOnRepeat(context -> context.applicationContext().rollback());
 *   flux.repeatWhen(repeat);
 * </code></pre>
 *
 * @param <T> Application context type
 */
public interface Repeat<T> extends Function<Flux<Long>, Publisher<Long>> {

  /**
   * Repeat function that repeats only if the predicate returns true.
   * @param predicate Predicate that determines if next repeat is performed
   * @return Repeat function with predicate
   */
  static <T> Repeat<T> onlyIf(Predicate<? super RepeatContext<T>> predicate) {
    return DefaultRepeat.create(predicate, Integer.MAX_VALUE);
  }

  /**
   * Repeat function that repeats once.
   * @return Repeat function for one repeat
   */
  static <T> Repeat<T> once() {
    return times(1);
  }

  /**
   * Repeat function that repeats n times.
   * @param n number of repeats
   * @return Repeat function for n repeats
   */
  static <T> Repeat<T> times(int n) {
    if (n < 0)
      throw new IllegalArgumentException("n should be >= 0");
    return DefaultRepeat.create(context -> true, n);
  }

  /**
   * Returns a repeat function with an application context that may be
   * used to perform any rollbacks before a repeat. This application
   * context is provided to any repeat predicate {@link #onlyIf(Predicate)},
   * custom backoff function {@link #backoff(Backoff)} and repeat
   * callback {@link #doOnRepeat(Consumer)}. All other properties of
   * this repeat function are retained in the returned instance.
   *
   * @param applicationContext Application context
   * @return repeat function with associated application context
   */
  Repeat<T> withApplicationContext(T applicationContext);

  /**
   * Returns a repeat function that invokes the provided onRepeat
   * callback before every repeat. The {@link RepeatContext} provided
   * to the callback contains the iteration and the any application
   * context set using {@link #withApplicationContext(Object)}.
   * All other properties of this repeat function are retained in the
   * returned instance.
   *
   * @param onRepeat callback to invoke before repeats
   * @return repeat function with callback
   */
  Repeat<T> doOnRepeat(Consumer<? super RepeatContext<T>> onRepeat);

  /**
   * Returns a repeat function with timeout. The timeout starts from
   * the instant that this function is applied. All other properties of
   * this repeat function are retained in the returned instance.
   * @param timeout timeout after which no new repeats are initiated
   * @return repeat function with timeout
   */
  Repeat<T> timeout(Duration timeout);

  /**
   * Returns a repeat function with backoff delay.
   * All other properties of this repeat function are retained in the
   * returned instance.
   *
   * @param backoff the backoff function to determine backoff delay
   * @return repeat function with backoff
   */
  Repeat<T> backoff(Backoff backoff);

  /**
   * Returns a repeat function that applies jitter to the backoff delay.
   * All other properties of this repeat function are retained in the
   * returned instance.
   *
   * @param jitter Jitter function to randomize backoff delay
   * @return repeat function with jitter for backoff
   */
  Repeat<T> jitter(Jitter jitter);

  /**
   * Returns a repeat function that uses the scheduler provided for
   * backoff delays. All other properties of this repeat function
   * are retained in the returned instance.
   * @param scheduler the scheduler for backoff delays
   * @return repeat function with backoff scheduler
   */
  Repeat<T> withBackoffScheduler(Scheduler scheduler);

  /**
   * Returns a repeat function with no backoff delay. This is the default.
   * All other properties of this repeat function are retained in the
   * returned instance.
   *
   * @return repeat function with no backoff delay
   */
  default Repeat<T> noBackoff() {
    return backoff(Backoff.zero());
  }

  /**
   * Returns a repeat function with fixed backoff delay.
   * All other properties of this repeat function are retained in the
   * returned instance.
   *
   * @param backoffInterval fixed backoff delay applied before every repeat
   * @return repeat function with fixed backoff delay
   */
  default Repeat<T> fixedBackoff(Duration backoffInterval) {
    return backoff(Backoff.fixed(backoffInterval));
  }

  /**
   * Returns a repeat function with exponential backoff delay.
   * All other properties of this repeat function are retained in the
   * returned instance.
   * <p>
   * Repeats are performed after a backoff interval of <code>firstBackoff * (2 ** n)</code>
   * where n is the next iteration number. If <code>maxBackoff</code> is not null, the maximum
   * backoff applied will be limited to <code>maxBackoff</code>.
   *
   * @param firstBackoff the delay for the first backoff, which is also used as the coefficient for subsequent backoffs
   * @param maxBackoff the maximum backoff delay before a repeat
   * @return repeat function with exponential backoff delay
   */
  default Repeat<T> exponentialBackoff(Duration firstBackoff, Duration maxBackoff) {
    return backoff(Backoff.exponential(firstBackoff, maxBackoff, 2, false));
  }

  /**
   * Returns a repeat function with full jitter backoff strategy.
   * All other properties of this repeat function are retained in the
   * returned instance.
   * <p>
   * Repeats are performed after a random backoff interval between  <code>firstBackoff</code> and
   * <code>firstBackoff * (2 ** n)</code> where n is the next iteration number. If <code>maxBackoff</code>
   * is not null, the maximum backoff applied will be limited to <code>maxBackoff</code>.
   *
   * @param firstBackoff the delay for the first backoff, which is also used as the coefficient for subsequent backoffs
   * @param maxBackoff the maximum backoff delay before a repeat
   * @return repeat function with full jitter backoff strategy
   */
  default Repeat<T> exponentialBackoffWithJitter(Duration firstBackoff, Duration maxBackoff) {
    return backoff(Backoff.exponential(firstBackoff, maxBackoff, 2, false)).jitter(Jitter.random());
  }

  /**
   * Returns a repeat function with random de-correlated jitter backoff strategy.
   * All other properties of this repeat function are retained in the
   * returned instance.
   * <p>
   * Repeats are performed after a backoff interval of <code>random_between(firstBackoff, prevBackoff * 3)</code>,
   * with a minimum value of <code>firstBackoff</code>. If <code>maxBackoff</code>
   * is not null, the maximum backoff applied will be limited to <code>maxBackoff</code>.
   *
   * @param firstBackoff the delay for the first backoff, also used as minimum backoff
   * @param maxBackoff the maximum backoff delay before a repeat
   * @return repeat function with de-correlated jitter backoff strategy
   */
  default Repeat<T> randomBackoff(Duration firstBackoff, Duration maxBackoff) {
    return backoff(Backoff.exponential(firstBackoff, maxBackoff, 3, true)).jitter(Jitter.random());
  }

  /**
   * Transforms the source into a repeating {@link Flux} based on the properties
   * configured for this function.
   * <p>
   * Example usage:
   * <pre><code>
   *    repeat = Repeat.times(n)
   *                   .exponentialBackoff(Duration.ofMillis(100), Duration.ofSeconds(60));
   *    flux.as(repeat);
   * </code></pre>
   *
   * @param source the source publisher
   * @return {@link Flux} with the repeat properties of this repeat function
   */
  default <S> Flux<S> apply(Publisher<S> source) {
    return Flux.from(source).repeatWhen(this);
  }
}
