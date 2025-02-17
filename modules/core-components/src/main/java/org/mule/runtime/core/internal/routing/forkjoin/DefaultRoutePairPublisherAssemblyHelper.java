/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.forkjoin;

import static org.mule.runtime.core.internal.event.EventQuickCopy.quickCopy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;

import static java.util.Optional.empty;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.event.AbstractEventContext;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.exception.MessagingException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

/**
 * Default implementation of {@link RoutePairPublisherAssemblyHelper} which takes care of completing the child contexts in case of
 * timeouts.
 */
class DefaultRoutePairPublisherAssemblyHelper implements RoutePairPublisherAssemblyHelper {

  private final Publisher<CoreEvent> publisherWithChildContext;
  private final BaseEventContext childContext;
  private final Scheduler completionScheduler;

  DefaultRoutePairPublisherAssemblyHelper(CoreEvent routeEvent, ReactiveProcessor chain, Scheduler completionScheduler) {
    // This sequence is equivalent to processWithChildContextDontComplete (used in the legacy version), only this way we can keep
    // track of the child context, which we will need later on.
    this.childContext = newChildContext(routeEvent, empty());
    this.publisherWithChildContext = processWithChildContext(routeEvent, chain, childContext);
    this.completionScheduler = completionScheduler;
  }

  @Override
  public Publisher<CoreEvent> getPublisherOnChildContext() {
    return publisherWithChildContext;
  }

  @Override
  public Publisher<CoreEvent> decorateTimeoutPublisher(Publisher<CoreEvent> timeoutPublisher) {
    // When the timeout happens, the subscription to the original publisher is cancelled, so the inner MessageProcessorChains
    // never finish and the child contexts are never completed, hence we have to complete them manually on timeout
    return Mono.from(timeoutPublisher)
        .doOnSuccess(completeRecursively(childContext, BaseEventContext::error));
  }

  private Consumer<CoreEvent> completeRecursively(BaseEventContext eventContext,
                                                  BiConsumer<BaseEventContext, MessagingException> forEachChild) {
    return some -> {
      completionScheduler.submit(() -> {
        // Tracks the child contexts first and then completes them, that way we are not retaining the read lock all the time.
        // We use a stack so we iterate them in reverse order, so that child contexts are always visited before their respective
        // parents. This assumes forEachChild visits on a parent-first strategy (which is currently the case).
        Deque<BaseEventContext> allContexts = new ArrayDeque<>();
        allContexts.push(eventContext);
        ((AbstractEventContext) eventContext).forEachChild(allContexts::push);

        while (!allContexts.isEmpty()) {
          BaseEventContext ctx = allContexts.pop();
          // Some context may already be terminated, as the children can propagate the termination up to their parents.
          if (!ctx.isTerminated()) {
            // It is important to swap the context so it has the right flow stack among other things.
            forEachChild.accept(ctx, new MessagingException(quickCopy(ctx, some), some.getError().get().getCause()));
          }
        }
      });
    };
  }
}
