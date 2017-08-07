/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing.forkjoin;

import static java.lang.Long.MAX_VALUE;
import static java.lang.String.format;
import static java.time.Duration.ofMillis;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.api.rx.Exceptions.unwrapCompositeException;
import static reactor.core.publisher.Flux.from;
import static reactor.util.concurrent.QueueSupplier.XS_BUFFER_SIZE;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy;
import org.mule.runtime.core.internal.routing.CompositeRoutingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Abstract {@link org.mule.runtime.core.api.routing.ForkJoinStrategy} that provides the base behavior for strategies that will
 * perform parallel invocation of {@link org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair}'s that wish to use the
 * following common behaviour:
 * <ul>
 * <li>Emit a single result event once all routes complete.
 * <li>Merge variables using a last-wins strategy.
 * <li>Use of an optional timeout.
 * <li>Delay error behavior, where all errors are collated and thrown as a composite exception.
 * </ul>
 */
public abstract class AbstractForkJoinStrategy implements ForkJoinStrategy {

  private static final long DEFAULT_TIMEOUT = MAX_VALUE;
  private final long timeout;

  public AbstractForkJoinStrategy() {
    this(DEFAULT_TIMEOUT);
  }

  public AbstractForkJoinStrategy(long timeout) {
    this.timeout = timeout;
  }

  @Override
  public Publisher<Event> forkJoin(Event original, Publisher<RoutingPair> forkPairs,
                                   ProcessingStrategy processingStrategy, int maxConcurrency, boolean delayErrors) {
    final AtomicInteger count = new AtomicInteger();
    final Event.Builder resultBuilder = Event.builder(original);


    return from(forkPairs)
        .flatMapSequential(pair -> processWithChildContext(addSequence(pair.getEvent(), count),
                                                           applyProcessingStrategy(processingStrategy, pair,
                                                                                   maxConcurrency),
                                                           Optional.empty()),
                           delayErrors, maxConcurrency, XS_BUFFER_SIZE)
        .collectList()
        .timeout(ofMillis(getTimeout()))
        .doOnNext(copyVars(resultBuilder))
        .map(createResultEvent(original, resultBuilder))
        .onErrorMap(handleErrors(original, count, delayErrors));
  }

  protected abstract Function<List<Event>, Event> createResultEvent(Event original, Event.Builder resultBuilder);

  private ReactiveProcessor applyProcessingStrategy(ProcessingStrategy processingStrategy, RoutingPair pair, int maxConcurrency) {
    if (maxConcurrency > 1) {
      return processingStrategy.onPipeline(pair.getProcessor());
    } else {
      return pair.getProcessor();
    }
  }

  private Function<Throwable, Throwable> handleErrors(Event original, AtomicInteger count, boolean delayErrors) {
    return throwable -> {
      if (throwable instanceof TimeoutException) {
        return new MessagingException(original,
                                      new DefaultMuleException(createStaticMessage(format("Timeout while waiting for route %d",
                                                                                          count.get() - 1)),
                                                               throwable));
      } else if (delayErrors) {
        // TODO MULE-10421 Define and implement how to deal with composite exceptions
        return new MessagingException(original, createCompositeRoutingException(throwable));
      } else {
        return throwable;
      }
    };
  }

  private CompositeRoutingException createCompositeRoutingException(Throwable throwable) {
    Map<String, Throwable> map = new HashMap<>();
    for (Throwable t : unwrapCompositeException(throwable)) {
      Event event = ((MessagingException) t).getEvent();
      map.put(Integer.toString(event.getGroupCorrelation().get().getSequence()).toString(),
              t instanceof MessagingException ? t.getCause() : t);
    }
    return new CompositeRoutingException(map);
  }


  private Consumer<List<Event>> copyVars(Event.Builder result) {
    return list -> list.stream()
        .forEach(event -> event.getVariables().forEach((key, value) -> {
          result.addVariable(key, value);
        }));
  }



  protected Event addSequence(Event event, AtomicInteger atomicLong) {
    return builder(event).groupCorrelation(Optional.of(GroupCorrelation.of(atomicLong.getAndIncrement()))).build();
  }

  protected long getTimeout() {
    return timeout;
  }
}
