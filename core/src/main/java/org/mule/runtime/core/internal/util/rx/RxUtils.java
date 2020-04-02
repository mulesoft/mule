/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.core.api.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.internal.processor.strategy.TransactionAwareStreamEmitterProcessingStrategyDecorator.popTxFromSubscriberContext;
import static org.mule.runtime.core.internal.processor.strategy.TransactionAwareStreamEmitterProcessingStrategyDecorator.pushTxToSubscriberContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.api.util.func.Once.ConsumeOnce;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Reactor specific utils
 */
public class RxUtils {

  private static final Logger LOGGER = getLogger(RxUtils.class);

  public static final String KEY_ON_NEXT_ERROR_STRATEGY = "reactor.onNextError.localStrategy";
  public static final String ON_NEXT_FAILURE_STRATEGY = "reactor.core.publisher.OnNextFailureStrategy$ResumeStrategy";

  /**
   * Defers the subscription of the <it>deferredSubscriber</it> until <it>triggeringSubscriber</it> subscribes. Once that occurs
   * the latter subscription will take place on the same context. For an example of this, look at
   * {@link org.mule.runtime.core.internal.routing.ChoiceRouter}
   * <p>
   * This serves its purpose in some in which the are two Fluxes, A and B, and are related in that in some part of A's reactor
   * chain, the processed event is published into a sink that belongs to B. Also, suppose that some of A's processors need to be
   * initialized in order to make the whole assembled chain work. In those cases, one may want to do A's subscription after it has
   * initialized, and once B has subscribed.
   * <p>
   * A -----> B's Sink -> B -------> downstream chain
   * <p>
   * In this method, A corresponds to <it>deferredSubscriber</it>; and B to <it>triggeringSubscriber</it>.
   *
   * @param <T> the element type of the downstream {@link Flux}
   * @param <U> the element type of the upstream {@link Flux}
   *
   * @param triggeringSubscriber the downstream {@link Flux}, whose subscription will trigger the subscription of the
   *        <it>deferredSubscriber</it> {@link Flux}, on the same context as the former one.
   * @param deferredSubscriber the upstream {@link Flux}, whose subscription will be deferred
   * @return the triggeringSubscriber {@link Flux}, decorated with the callback that will perform this deferred subscription.
   * @since 4.3
   */
  public static <T, U> Flux<T> subscribeFluxOnPublisherSubscription(Flux<T> triggeringSubscriber,
                                                                    Flux<U> deferredSubscriber) {
    return triggeringSubscriber
        .compose(eventPub -> subscriberContext()
            .flatMapMany(ctx -> eventPub.doOnSubscribe(s -> deferredSubscriber
                .subscriberContext(ctx)
                .subscribe())));
  }

  /**
   * As {@link #subscribeFluxOnPublisherSubscription(Flux, Flux)}, but also propagates completion and cancellation events in the
   * upstream to the downstream.
   * <p>
   * Internal state is kept so that any pending items still being processed are waited for before propagating any completion or
   * cancellation event. This implementation will not reject new items after receiving the completion or cancellation event. It is
   * up to the caller to avoid sending new items to be processed when completion or cancellation is expected.
   *
   * @param upstream the source of the items, completion of cancellation events to be processed and propagated downstream.
   * @param downstream the downstream that will receive the items, completion of cancellation events, as well as trigger the
   *        subscription to upstream.
   * @param transformer the data transformation to be done on the items of upstream. If any items are in any operators of this
   *        transformation when a completion or cancellation event is received, that event will wait for any in-flight events to
   *        finish.
   * @param completionCallback how a completion event will be triggered on the downstream.
   * @param errorCallback how a cancellation event will be triggered on the downstream.
   * @return an enriched downstream where items and events will be triggered according to the rules defined for this method.
   */
  public static <T, U> Publisher<T> propagateCompletion(Publisher<U> upstream, Publisher<T> downstream,
                                                        Function<Publisher<U>, Publisher<T>> transformer,
                                                        CheckedRunnable completionCallback,
                                                        CheckedConsumer<Throwable> errorCallback) {
    requireNonNull(upstream, "'upstream' must not be null");
    requireNonNull(downstream, "'downstream' must not be null");
    requireNonNull(transformer, "'transformer' must not be null");
    requireNonNull(completionCallback, "'completionCallback' must not be null");
    requireNonNull(errorCallback, "'errorCallback' must not be null");

    return doPropagateCompletion(upstream, downstream, transformer,
                                 new AtomicInteger(0),
                                 Once.of(completionCallback), Once.of(errorCallback),
                                 () -> null);
  }

  /**
   * As {@link #subscribeFluxOnPublisherSubscription(Flux, Flux)}, but also propagates completion and cancellation events in the
   * upstream to the downstream.
   * <p>
   * Internal state is kept so that any pending items still being processed are waited for before propagating any completion or
   * cancellation event, while it may also wait for executions happening aoutside of this method. This implementation will not
   * reject new items after receiving the completion or cancellation event. It is up to the caller to avoid sending new items to
   * be processed when completion or cancellation is expected.
   * <p>
   * Upon termination of the {@code upstream}, the downstream will be terminated as well when there are no more inflight events,
   * or after {@code completionTimeoutMillis} have elapsed, canceling the inflight events.
   *
   * @param upstream the source of the items, completion of cancellation events to be processed and propagated downstream.
   * @param downstream the downstream that will receive the items, completion of cancellation events, as well as trigger the
   *        subscription to upstream.
   * @param transformer the data transformation to be done on the items of upstream. If any items are in any operators of this
   *        transformation when a completion or cancellation event is received, that event will wait for any in-flight events to
   *        finish.
   * @param inflightCounter a counter where externally executing items may be accounted for, to avoid premature completion of the
   *        downstream flux.
   * @param completionCallback how a completion event will be triggered on the downstream.
   * @param errorCallback how a cancellation event will be triggered on the downstream.
   * @return an enriched downstream where items and events will be triggered according to the rules defined for this method.
   */
  public static <T, U> Publisher<T> propagateCompletion(Publisher<U> upstream, Publisher<T> downstream,
                                                        Function<Publisher<U>, Publisher<T>> transformer,
                                                        AtomicInteger inflightCounter,
                                                        CheckedRunnable completionCallback,
                                                        CheckedConsumer<Throwable> errorCallback) {
    requireNonNull(upstream, "'upstream' must not be null");
    requireNonNull(downstream, "'downstream' must not be null");
    requireNonNull(transformer, "'transformer' must not be null");
    requireNonNull(completionCallback, "'completionCallback' must not be null");
    requireNonNull(errorCallback, "'errorCallback' must not be null");

    return doPropagateCompletion(upstream, downstream, transformer,
                                 inflightCounter,
                                 Once.of(completionCallback), Once.of(errorCallback),
                                 () -> null);
  }

  /**
   * As {@link #subscribeFluxOnPublisherSubscription(Flux, Flux)}, but also propagates completion and cancellation events in the
   * upstream to the downstream.
   * <p>
   * Internal state is kept so that any pending items still being processed are waited for before propagating any completion or
   * cancellation event. This implementation will not reject new items after receiving the completion or cancellation event. It is
   * up to the caller to avoid sending new items to be processed when completion or cancellation is expected.
   * <p>
   * Upon termination of the {@code upstream}, the downstream will be terminated as well when there are no more inflight events,
   * or after {@code completionTimeoutMillis} have elapsed, canceling the inflight events.
   *
   * @param upstream the source of the items, completion of cancellation events to be processed and propagated downstream.
   * @param downstream the downstream that will receive the items, completion of cancellation events, as well as trigger the
   *        subscription to upstream.
   * @param transformer the data transformation to be done on the items of upstream. If any items are in any operators of this
   *        transformation when a completion or cancellation event is received, that event will wait for any in-flight events to
   *        finish.
   * @param completionCallback how a completion event will be triggered on the downstream.
   * @param errorCallback how a cancellation event will be triggered on the downstream.
   * @param completionTimeoutMillis how long to wait for pending items to finish processing before actually propagating the
   *        completion or cancellation downstream.
   * @param delayedExecutor the executor that will delay the completion or cancellation propagation when there are pending items
   * @return an enriched downstream where items and events will be triggered according to the rules defined for this method.
   */
  public static <T, U> Publisher<T> propagateCompletion(Publisher<U> upstream, Publisher<T> downstream,
                                                        Function<Publisher<U>, Publisher<T>> transformer,
                                                        CheckedRunnable completionCallback,
                                                        CheckedConsumer<Throwable> errorCallback,
                                                        long completionTimeoutMillis, ScheduledExecutorService delayedExecutor) {
    requireNonNull(upstream, "'upstream' must not be null");
    requireNonNull(downstream, "'downstream' must not be null");
    requireNonNull(transformer, "'transformer' must not be null");
    requireNonNull(completionCallback, "'completionCallback' must not be null");
    requireNonNull(errorCallback, "'errorCallback' must not be null");
    requireNonNull(delayedExecutor, "'delayedExecutor' must not be null");

    final RunOnce completer = Once.of(completionCallback);
    final ConsumeOnce<Throwable> errorForwarder = Once.of(errorCallback);

    return doPropagateCompletion(upstream, downstream, transformer,
                                 new AtomicInteger(0), completer, errorForwarder,
                                 () -> delayedExecutor.schedule(() -> completer.runOnce(), completionTimeoutMillis,
                                                                MILLISECONDS));
  }

  private static <T, U> Publisher<T> doPropagateCompletion(Publisher<U> upstream, Publisher<T> downstream,
                                                           Function<Publisher<U>, Publisher<T>> transformer,
                                                           AtomicInteger inflightCounter,
                                                           final RunOnce completer, final ConsumeOnce<Throwable> errorForwarder,
                                                           final Supplier<ScheduledFuture<?>> scheduleCompletion) {
    AtomicBoolean upstreamComplete = new AtomicBoolean(false);
    AtomicReference<Throwable> upstreamError = new AtomicReference<>();

    AtomicReference<ScheduledFuture<?>> scheduledCompletion = new AtomicReference<>();

    Flux<T> enrichedUpstream = Flux.from(upstream)
        .doOnNext(s -> inflightCounter.incrementAndGet())
        .transform(transformer)
        .doOnComplete(() -> {
          upstreamComplete.set(true);

          if (inflightCounter.get() == 0) {
            completer.runOnce();
          } else {
            scheduledCompletion.set(scheduleCompletion.get());
          }
        })
        .doOnError(t -> {
          upstreamError.set(t);
          errorForwarder.consumeOnce(t);
        });

    return subscribeFluxOnPublisherSubscription(Flux.from(downstream)
        .doOnNext(s -> {
          if (inflightCounter.decrementAndGet() == 0 && upstreamComplete.get()) {
            completer.runOnce();
            final ScheduledFuture<?> scheduledFuture = scheduledCompletion.get();
            if (scheduledFuture != null) {
              scheduledFuture.cancel(true);
            }
          }
        }),
                                                enrichedUpstream);
  }

  /**
   * Transform a given {@link Publisher} using a {@link ReactiveProcessor}. Primarily for use in the implementation of
   * {@link ReactiveProcessor} in other class-loaders.
   *
   * @param publisher the publisher to transform
   * @param processor the processor to transform publisher with
   * @return the transformed publisher
   * @since 4.3
   */
  public static Publisher<CoreEvent> transform(Publisher<CoreEvent> publisher, ReactiveProcessor processor) {
    return Flux.from(publisher).transform(processor);
  }

  /**
   * Transform a given {@link Publisher} using a mapper function. Primarily for use in the implementation of mapping in other
   * class-loaders.
   *
   * @param publisher the publisher to transform
   * @param mapper the mapper to map publisher items with
   * @return the transformed publisher
   * @since 4.3
   */
  public static Publisher<CoreEvent> map(Publisher<CoreEvent> publisher, Function<CoreEvent, CoreEvent> mapper) {
    return Flux.from(publisher).map(mapper);
  }

  /**
   * Perform processing using the provided {@link Function} for each {@link CoreEvent}. Primarily for use in the implementation of
   * {@link ReactiveProcessor} in other class-loaders.
   *
   * @param publisher the publisher to transform
   * @param function the function to apply to each event.
   * @param component the component that implements this functionality.
   * @return the transformed publisher
   * @since 4.3
   */
  public static Publisher<CoreEvent> flatMap(Publisher<CoreEvent> publisher,
                                             Function<CoreEvent, Publisher<CoreEvent>> function, Component component) {
    return Flux.from(publisher)
        .flatMap(event -> from(function.apply(event))
            .onErrorMap(e -> !(e instanceof MessagingException), e -> new MessagingException(event, e, component)));
  }

  /**
   * Creates a new {@link Publisher} that will emit the given {@code event}, publishing it on the given {@code executor}.
   *
   * @param event the {@link CoreEvent} to emit
   * @param executor the thread pool where the event will be published.
   * @return the created publisher
   * @since 4.3
   */
  public static Publisher<CoreEvent> justPublishOn(CoreEvent event, ExecutorService executor) {
    return Flux.just(event).publishOn(fromExecutorService(executor));
  }

  /**
   * Creates a {@link Supplier} that on {@link Supplier#get()} invocation will create and subscribe a new {@link Flux} configured
   * through the given {@code configurer}.
   *
   * @param configurer a {@link Function} that receives the blank {@link Flux} and returns a configured one
   * @param <T> the Flux generic type
   * @return a {@link Supplier} that returns a new {@link FluxSink} each time.
   */
  public static <T> Supplier<FluxSink<T>> createFluxSupplier(Function<Flux<T>, Flux<?>> configurer) {
    return () -> {
      final FluxSinkRecorder<T> sinkRef = new FluxSinkRecorder<>();

      final Flux<T> upstream;
      if (isTransactionActive()) {
        upstream = sinkRef.flux()
            .subscriberContext(popTxFromSubscriberContext());
      } else {
        upstream = sinkRef.flux();
      }

      Flux<?> flux = configurer.apply(upstream);

      if (isTransactionActive()) {
        flux = flux.subscriberContext(pushTxToSubscriberContext(configurer.toString()));
      }

      flux.subscribe(null, e -> LOGGER.error("Exception reached subscriber for " + configurer.toString(), e));
      return sinkRef.getFluxSink();
    };
  }

  /**
   * Returns a {@link RoundRobinFluxSinkSupplier} of the given {@code size}. The underlying fluxes are configured through the
   * given {@code configurer}.
   *
   * @param configurer a {@link Function} that receives the blank {@link Flux} and returns a configured one
   * @param size the round robin size
   * @param <T> the Flux generic type
   * @return a new {@link FluxSinkSupplier}
   */
  public static <T> FluxSinkSupplier<T> createRoundRobinFluxSupplier(Function<Flux<T>, Flux<?>> configurer, int size) {
    final Supplier<FluxSink<T>> factory = createFluxSupplier(configurer);
    return new TransactionAwareFluxSinkSupplier<>(factory,
                                                  new RoundRobinFluxSinkSupplier<>(size, factory));
  }
}
