/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;
import reactor.util.Loggers;

public abstract class AbstractRetry<T, S> implements Function<Flux<S>, Publisher<Long>> {

  static final Logger log = Loggers.getLogger(AbstractRetry.class);

  static final BackoffDelay RETRY_EXHAUSTED = new BackoffDelay(Duration.ofSeconds(-1));

  final int maxIterations;
  final Duration timeout;
  final Backoff backoff;
  final Jitter jitter;
  final Scheduler backoffScheduler;
  final T applicationContext;

  AbstractRetry(int maxIterations,
                Duration timeout,
                Backoff backoff,
                Jitter jitter,
                Scheduler backoffScheduler,
                T applicationContext) {
    this.maxIterations = maxIterations;
    this.timeout = timeout;
    this.backoff = backoff;
    this.jitter = jitter;
    this.backoffScheduler = backoffScheduler;
    this.applicationContext = applicationContext;
  }

  Instant calculateTimeout() {
    return timeout != null ? Instant.now().plus(timeout) : Instant.MAX;
  }

  BackoffDelay calculateBackoff(Context<T> retryContext, Instant timeoutInstant) {
    BackoffDelay nextBackoff = backoff.apply(retryContext);
    Duration backoff = jitter.apply(nextBackoff);
    Duration minBackoff = nextBackoff.min;
    Duration maxBackoff = nextBackoff.max;
    if (maxBackoff != null)
      backoff = backoff.compareTo(maxBackoff) < 0 ? backoff : maxBackoff;
    if (minBackoff != null)
      backoff = backoff.compareTo(minBackoff) > 0 ? backoff : minBackoff;
    if (retryContext.iteration() > maxIterations || Instant.now().plus(backoff).isAfter(timeoutInstant))
      return RETRY_EXHAUSTED;
    else
      return new BackoffDelay(minBackoff, maxBackoff, backoff);
  }

  Publisher<Long> retryMono(Duration delay) {
    if (delay == Duration.ZERO)
      return Mono.just(0L);
    else if (backoffScheduler == null)
      return Mono.delay(delay);
    else
      return Mono.delay(delay, backoffScheduler);
  }
}
