/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.util.context.Context.empty;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.event.EventInternalContextResolver;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import reactor.core.publisher.Flux;
import reactor.util.context.Context;

/**
 * Router with {@link FirstSuccessful} routing logic.
 *
 * The routing chain isolation is implemented using two {@link reactor.core.publisher.FluxSink}s, one for the entry inside the
 * routing chain, and another for publishing successful events, or exhaustion errors.
 *
 * @since 4.2.3, 4.3.0
 */
class FirstSuccessfulRouter {

  private final Logger LOGGER = getLogger(FirstSuccessfulRouter.class);
  private static final String FIRST_SUCCESSFUL_START_EVENT = "_firstSuccessfulStartEvent";

  private final Component owner;
  private final Flux<CoreEvent> upstreamFlux;
  private final List<Flux<CoreEvent>> innerFluxes = new ArrayList<>();
  private final List<FluxSinkRecorder<CoreEvent>> innerRecorders;
  private final Flux<CoreEvent> downstreamFlux;
  private final FluxSinkRecorder<Either<Throwable, CoreEvent>> downstreamRecorder = new FluxSinkRecorder<>();
  private final EventInternalContextResolver<Stack<CoreEvent>> nextExecutionContextResolver =
      new EventInternalContextResolver<>(FIRST_SUCCESSFUL_START_EVENT, Stack::new);
  private final AtomicReference<Context> downstreamContextReference = new AtomicReference<>(empty());

  // When using a first successful scope in a blocking flow (for example, calling the owner flow with a Processor#process call),
  // this leads to a reactor completion signal being emitted while the event is being re-injected for retrials. This is solved by
  // deferring the downstream publisher completion until all events have evacuated the scope.
  private final AtomicInteger inflightEvents = new AtomicInteger(0);
  private final AtomicBoolean completeDeferred = new AtomicBoolean(false);

  private boolean isOriginalError(Error newError, Optional<Error> originalError) {
    return originalError.map(error -> error.equals(newError)).orElse(false);
  }

  public FirstSuccessfulRouter(Component owner, Publisher<CoreEvent> publisher, List<ProcessorRoute> routes) {
    this.owner = owner;

    innerRecorders = routes.stream().map(x -> new FluxSinkRecorder<CoreEvent>()).collect(toList());

    // Upstream side of until successful chain. Injects events into retrial chain.
    upstreamFlux = Flux.from(publisher)
        .doOnNext(event -> {
          // Inject event into retrial execution chain
          inflightEvents.getAndIncrement();
          innerRecorders.get(0).next(startEvent(event));
        })
        .doOnComplete(() -> {
          if (inflightEvents.get() == 0) {
            completeRouter();
          } else {
            completeDeferred.set(true);
          }
        });

    for (int i = 0; i < routes.size(); i++) {
      FluxSinkRecorder<CoreEvent> nextRecorder = i < routes.size() - 1 ? innerRecorders.get(i + 1) : null;
      innerFluxes.add(createMidFlux(routes.get(i), innerRecorders.get(i), ofNullable(nextRecorder)));
    }

    downstreamFlux = Flux.<Either<Throwable, CoreEvent>>create(sink -> {
      downstreamRecorder.accept(sink);
      // Upstream chains subscription delayed until downstream sink is recorded. This handles the transaction-enabled case, in
      // which the subscribing thread is the one that runs the whole chain. Check UntilSuccessfulRouter for more implementation details.
      subscribeUpstreamChains(downstreamContextReference.get());

    })
        .doOnNext(event -> inflightEvents.decrementAndGet())
        .map(getScopeResultMapper());

  }

  private Function<Either<Throwable, CoreEvent>, CoreEvent> getScopeResultMapper() {
    return either -> {
      if (either.isLeft()) {
        throw propagate(either.getLeft());
      } else {
        return either.getRight();
      }
    };
  }

  private void completeRouter() {
    for (FluxSinkRecorder<CoreEvent> innerRecorder : innerRecorders) {
      innerRecorder.complete();
    }
    downstreamRecorder.complete();
  }

  private void completeRouterIfNecessary() {
    if (completeDeferred.get() && inflightEvents.get() == 0) {
      completeRouter();
    }
  }

  /**
   * Assembles and returns the downstream {@link Publisher<CoreEvent>}.
   *
   * @return the successful {@link CoreEvent} or routing errors {@link Publisher}
   */
  Publisher<CoreEvent> getDownstreamPublisher() {
    return downstreamFlux
        .compose(downstreamPublisher -> subscriberContext().flatMapMany(downstreamContext -> downstreamPublisher
            .doOnSubscribe(s -> downstreamContextReference.set(downstreamContext))));
  }

  private void subscribeUpstreamChains(Context downstreamContext) {
    for (Flux<CoreEvent> innerFlux : innerFluxes) {
      innerFlux.subscriberContext(downstreamContext).subscribe();
    }
    upstreamFlux.subscriberContext(downstreamContext).subscribe();
  }


  private CoreEvent startEvent(CoreEvent event) {
    Stack<CoreEvent> nextEventContainer = nextExecutionContextResolver.getCurrentContextFromEvent(event);
    nextEventContainer.push(event);
    return nextExecutionContextResolver.eventWithContext(event, nextEventContainer);
  }

  private void executeNext(Optional<FluxSinkRecorder<CoreEvent>> next, CoreEvent event, Throwable error) {
    Stack<CoreEvent> nextEventContainer = nextExecutionContextResolver.getCurrentContextFromEvent(event);
    CoreEvent nextEvent = nextEventContainer.pop();
    // If there is another route to execute, use it. If there isn't, then finish with an error
    if (next.isPresent()) {
      next.get().next(startEvent(nextEvent));
    } else {
      downstreamRecorder.next(left(error, CoreEvent.class));
    }
  }

  private Flux<CoreEvent> createMidFlux(ProcessorRoute route, FluxSinkRecorder<CoreEvent> innerRecorder,
                                        Optional<FluxSinkRecorder<CoreEvent>> next) {
    return innerRecorder.flux()
        .transform(innerPublisher -> applyWithChildContext(innerPublisher, route.getProcessor(), of(owner.getLocation())))
        .doOnNext(successfulEvent -> {
          // If event finishes with error, then it must be treated as an error
          if (successfulEvent.getError().isPresent()) {
            CoreEvent originalEvent = nextExecutionContextResolver.getCurrentContextFromEvent(successfulEvent).peek();
            if (!isOriginalError(successfulEvent.getError().get(), originalEvent.getError())) {
              executeNext(next, successfulEvent, successfulEvent.getError().get().getCause());
              return;
            }
          }
          // Scope execution was successful
          inflightEvents.decrementAndGet();
          completeRouterIfNecessary();
          Stack<CoreEvent> nextEventContainer = nextExecutionContextResolver.getCurrentContextFromEvent(successfulEvent);
          nextEventContainer.pop();
          downstreamRecorder
              .next(right(Throwable.class, nextExecutionContextResolver.eventWithContext(successfulEvent, nextEventContainer)));
        }).onErrorContinue((error, object) -> {
          if (object instanceof CoreEvent) {
            executeNext(next, (CoreEvent) object, error);
          } else if (error instanceof MessagingException) {
            executeNext(next, ((MessagingException) error).getEvent(), error);
          }
        });
  }

}
