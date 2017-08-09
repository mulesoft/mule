/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing.forkjoin;

import static java.lang.Long.MAX_VALUE;
import static java.time.Duration.ofMillis;
import static java.util.Optional.empty;
import static org.mule.runtime.core.api.Event.builder;
import static org.mule.runtime.core.api.processor.MessageProcessors.processWithChildContext;
import static org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair.of;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.message.ErrorBuilder;
import org.mule.runtime.core.api.message.GroupCorrelation;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy;
import org.mule.runtime.core.api.routing.ForkJoinStrategy.RoutingPair;
import org.mule.runtime.core.api.routing.ForkJoinStrategyFactory;
import org.mule.runtime.core.internal.routing.CompositeRoutingException;
import org.mule.runtime.core.internal.routing.RoutingResult;

import org.reactivestreams.Publisher;
import reactor.core.scheduler.Schedulers;

/**
 * Abstract {@link org.mule.runtime.core.api.routing.ForkJoinStrategy} that provides the base behavior for strategies that will
 * perform parallel invocation of {@link RoutingPair}'s that wish to use the following common behaviour:
 * <ul>
 * <li>Emit a single result event once all routes complete.
 * <li>Merge variables using a last-wins strategy.
 * <li>Use of an optional timeout.
 * <li>Delay error behavior, where all errors are collated and thrown as a composite exception.
 * </ul>
 */
public abstract class AbstractForkJoinStrategyFactory implements ForkJoinStrategyFactory {

  public static final String TIMEOUT_EXCEPTION_DESCRIPTION = "Route Timeout";
  public static final String TIMEOUT_EXCEPTION_DETAILED_DESCRIPTION_PREFIX = "Timeout while processing route/part:";

  @Override
  public ForkJoinStrategy createForkJoinStrategy(ProcessingStrategy processingStrategy, int maxConcurrency, boolean delayErrors,
                                                 long timeout, Scheduler timeoutScheduler, ErrorType timeoutErrorType) {
    reactor.core.scheduler.Scheduler reatorTimeoutScheduler = Schedulers.fromExecutorService(timeoutScheduler);
    return (original, routingPairs) -> {
      final AtomicInteger count = new AtomicInteger();
      final Event.Builder resultBuilder = Event.builder(original);
      return from(routingPairs)
          .map(addSequence(count))
          .flatMap(processRoutePair(processingStrategy, maxConcurrency, delayErrors, timeout, reatorTimeoutScheduler,
                                    timeoutErrorType),
                   maxConcurrency)
          .collectList()
          .doOnNext(checkedConsumer(list -> {
            if (list.stream().anyMatch(event -> event.getError().isPresent())) {
              throw new MessagingException(original, createCompositeRoutingException(list));
            }
          }))
          .doOnNext(copyVars(resultBuilder))
          .map(createResultEvent(original, resultBuilder));
    };
  }

  /**
   * Template method to be implemented by implementations that defines how the list of result {@link Event}'s should be aggregated
   * into a result {@link Event}
   * 
   * @param original the original event
   * @param resultBuilder a result builder with the current state of result event builder including flow variable
   * @return the result event
   */
  protected abstract Function<List<Event>, Event> createResultEvent(Event original, Event.Builder resultBuilder);

  private Function<RoutingPair, RoutingPair> addSequence(AtomicInteger count) {
    return pair -> of(builder(pair.getEvent()).groupCorrelation(Optional.of(GroupCorrelation.of(count.getAndIncrement())))
        .build(), pair.getRoute());
  }

  private Function<RoutingPair, Publisher<? extends Event>> processRoutePair(ProcessingStrategy processingStrategy,
                                                                             int maxConcurrency,
                                                                             boolean delayErrors, long timeout,
                                                                             reactor.core.scheduler.Scheduler timeoutScheduler,
                                                                             ErrorType timeoutErrorType) {

    return pair -> {
      ReactiveProcessor route = publisher -> from(publisher)
          .transform(pair.getRoute())
          .timeout(ofMillis(timeout),
                   defer(() -> delayErrors ? just(createTimeoutErrorEvent(timeoutErrorType, pair))
                       : error(new TimeoutException(TIMEOUT_EXCEPTION_DETAILED_DESCRIPTION_PREFIX + " '"
                           + pair.getEvent().getGroupCorrelation().get().getSequence() + "'"))),
                   timeoutScheduler);
      return from(processWithChildContext(pair.getEvent(),
                                          applyProcessingStrategy(processingStrategy, route, maxConcurrency), empty()))
                                              .onErrorResume(MessagingException.class,
                                                             me -> delayErrors ? just(me.getEvent()) : error(me));
    };
  }



  private ReactiveProcessor applyProcessingStrategy(ProcessingStrategy processingStrategy, ReactiveProcessor processor,
                                                    int maxConcurrency) {
    if (maxConcurrency > 1) {
      return processingStrategy.onPipeline(processor);
    } else {
      return processor;
    }
  }

  private Event createTimeoutErrorEvent(ErrorType timeoutErrorType, RoutingPair pair) {
    return Event.builder(pair.getEvent()).message(Message.of(null))
        .error(ErrorBuilder.builder().errorType(timeoutErrorType)
            .exception(new TimeoutException()).description(TIMEOUT_EXCEPTION_DESCRIPTION)
            .detailedDescription(TIMEOUT_EXCEPTION_DETAILED_DESCRIPTION_PREFIX + " '"
                + pair.getEvent().getGroupCorrelation().get().getSequence() + "'")
            .build())
        .build();
  }

  private CompositeRoutingException createCompositeRoutingException(List<Event> results) {
    Map<String, Message> successMap = new LinkedHashMap<>();
    Map<String, Error> errorMap = new LinkedHashMap<>();

    for (Event event : results) {
      String key = Integer.toString(event.getGroupCorrelation().get().getSequence());
      if (event.getError().isPresent()) {
        errorMap.put(key, event.getError().get());
      } else {
        successMap.put(key, event.getMessage());
      }
    }
    return new CompositeRoutingException(new RoutingResult(successMap, errorMap));
  }

  private Consumer<List<Event>> copyVars(Event.Builder result) {
    return list -> list.stream()
        .forEach(event -> event.getVariableNames().stream()
            .forEach(name -> result.addVariable(name, event.getVariable(name))));
  }

}
