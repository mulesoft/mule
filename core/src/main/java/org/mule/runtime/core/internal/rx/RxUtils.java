/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.rx;

import static reactor.core.publisher.Mono.subscriberContext;
import org.mule.runtime.core.api.event.CoreEvent;

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
   */
  public static Flux<CoreEvent> subscribeFluxOnPublisherSubscription(Flux<CoreEvent> triggeringSubscriber,
                                                                     Flux<CoreEvent> deferredSubscriber) {
    return triggeringSubscriber
        .compose(eventPub -> subscriberContext()
            .flatMapMany(ctx -> eventPub.doOnSubscribe(s -> deferredSubscriber.subscriberContext(ctx).subscribe())));
  }
}
