package org.mule.runtime.core.internal.rx;

import static reactor.core.publisher.Mono.subscriberContext;
import org.mule.runtime.core.api.event.CoreEvent;

import reactor.core.publisher.Flux;

/**
 * Reactor specific utils
 */
public class RxUtils {

  // TODO: Improve this JavaDoc. It's very unclear and redundant.
  /**
   * Defers the subscription of the <it>deferredSubscriber</it> until <it>subscriptionTrigger</it> subscribes. When that happens,
   * the latter subscription will take place on the same context. For an example of this, look at
   * {@link org.mule.runtime.core.internal.routing.ChoiceRouter}
   *
   * @param subscriptionTrigger The {@link Flux} whose subscription will trigger the subscription of the
   *        <it>deferredSubscriber</it> {@link Flux}, on the same context as the former one.
   * @param deferredSubscriber The {@link Flux} that whose subscription will be deferred
   * @return the subscriptionTrigger {@link Flux}, decorated with the callback that will perform this deferred subscription.
   */
  public static Flux<CoreEvent> subscribeFluxOnPublisherSubscription(Flux<CoreEvent> subscriptionTrigger,
                                                                     Flux<CoreEvent> deferredSubscriber) {
    return subscriptionTrigger
        .compose(eventPub -> subscriberContext()
            .flatMapMany(ctx -> eventPub.doOnSubscribe(s -> deferredSubscriber.subscriberContext(ctx).subscribe())));
  }
}
