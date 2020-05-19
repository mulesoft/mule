/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static java.util.Collections.newSetFromMap;
import static reactor.core.publisher.Mono.from;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.exception.MessagingException;

import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Reactor specific utils
 */
public class RxUtils {

  /**
   * Defers the subscription of the <it>deferredSubscriber</it> until <it>triggeringSubscriber</it> subscribes. Once that occurs
   * the latter subscription will take place on the same context. For an example of this, look at
   * {@link org.mule.runtime.core.internal.routing.ChoiceRouter}
   *
   * This serves its purpose in some in which the are two Fluxes, A and B, and are related in that in some part of A's reactor
   * chain, the processed event is published into a sink that belongs to B. Also, suppose that some of A's processors need to be
   * initialized in order to make the whole assembled chain work. In those cases, one may want to do A's subscription after it has
   * initialized, and once B has subscribed.
   * 
   * A -----> B's Sink -> B -------> downstream chain
   *
   * In this method, A corresponds to <it>deferredSubscriber</it>; and B to <it>triggeringSubscriber</it>.
   *
   * @param triggeringSubscriber the {@link Flux} whose subscription will trigger the subscription of the
   *        <it>deferredSubscriber</it> {@link Flux}, on the same context as the former one.
   * @param deferredSubscriber the {@link Flux} whose subscription will be deferred
   * @return the triggeringSubscriber {@link Flux}, decorated with the callback that will perform this deferred subscription.
   * @since 4.3
   */
  public static Flux<CoreEvent> subscribeFluxOnPublisherSubscription(Flux<CoreEvent> triggeringSubscriber,
                                                                     Flux<CoreEvent> deferredSubscriber) {
    return triggeringSubscriber
        .compose(eventPub -> subscriberContext()
            .flatMapMany(ctx -> eventPub.doOnSubscribe(s -> deferredSubscriber.subscriberContext(ctx).subscribe())));
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
   * Transforms the upstream {@link Publisher} using the given processor, and maintaining an in-flight events counter
   * in order to delay error sink completion until it's zero.
   *
   * @param upstream the upstream {@link Publisher}
   * @param errorSinkPublisher sink to merge with after completer transformer
   * @param processor the processor to transform the upstream publisher with
   * @param postProcessorTransformer transformer to be applied after the above processor
   * @param errorSinkCompleter callback to complete the error sink. It's applied onComplete or deferred until the last
   *                           in-flight event onNext
   * @param postCompleteTransformer transformer to be applied after merging with errorSinkCompleter
   * @param keyExtractor the composed flow will be merged with the errorSinkCompleter, and so we are going to need filtering
   *                     with a distinct. This parameter is used to extract the key for each element and do the mentioned filtering
   * @return the transformed {@link Publisher}
   */
  public static <T, U, TKey> Publisher<T> applyWaitingInflightEvents(Publisher<T> upstream,
                                                                     Publisher<Either<U, T>> errorSinkPublisher,
                                                                     Function<Publisher<T>, Publisher<T>> processor,
                                                                     Function<Publisher<T>, Publisher<Either<U, T>>> postProcessorTransformer,
                                                                     CheckedRunnable errorSinkCompleter,
                                                                     Function<Publisher<Either<U, T>>, Publisher<T>> postCompleteTransformer,
                                                                     Function<T, TKey> keyExtractor) {
    final AtomicInteger inflightEvents = new AtomicInteger(0);
    final AtomicBoolean deferredCompletion = new AtomicBoolean(false);
    Set<TKey> seenElements = newSetFromMap(new WeakHashMap<>());

    return Flux.from(upstream)
        .doOnNext(eventCtx -> inflightEvents.incrementAndGet())
        .transform(processor)
        .compose(postProcessorTransformer)
        .doOnComplete(() -> {
          if (inflightEvents.get() == 0) {
            errorSinkCompleter.run();
          } else {
            deferredCompletion.set(true);
          }
        })
        // This Either here is used to propagate errors. If the error is sent directly through the merged with Flux,
        // it will be cancelled, ignoring the onErrorContinue of the parent Flux.
        .mergeWith(errorSinkPublisher)
        .compose(postCompleteTransformer)
        .distinct(keyExtractor, () -> seenElements)
        .doOnNext(eventCtx -> {
          int inflightEventsCount = inflightEvents.decrementAndGet();
          if (inflightEventsCount == 0 && deferredCompletion.compareAndSet(true, false)) {
            errorSinkCompleter.run();
          }
        });
  }
}
