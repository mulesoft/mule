/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Optional.of;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.metadata.DataType.MULE_MESSAGE;
import static org.mule.runtime.api.metadata.DataType.NUMBER;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.create;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.util.context.Context.empty;

import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.message.ItemSequenceInfo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.event.EventInternalContextResolver;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurer;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class ForeachRouter {

  private static final Logger LOGGER = getLogger(ForeachRouter.class);

  private static final String MULE_FOREACH_CONTEXT_KEY = "mule.foreach.router.foreachContext";
  static final String MAP_NOT_SUPPORTED_MESSAGE =
      "Foreach does not support 'java.util.Map' with no collection expression. To iterate over Map entries use '#[dw::core::Objects::entrySet(payload)]'";
  private final EventInternalContextResolver<Map<String, ForeachContext>> foreachContextResolver;
  private final Foreach owner;

  private Flux<CoreEvent> upstreamFlux;
  private Flux<CoreEvent> innerFlux;
  private Flux<CoreEvent> downstreamFlux;
  private final FluxSinkRecorder<CoreEvent> innerRecorder = new FluxSinkRecorder<>();
  private final FluxSinkRecorder<Either<Throwable, CoreEvent>> downstreamRecorder = new FluxSinkRecorder<>();
  private final AtomicReference<Context> downstreamCtxReference = new AtomicReference(empty());

  private final AtomicInteger inflightEvents = new AtomicInteger(0);
  private final AtomicBoolean completeDeferred = new AtomicBoolean(false);

  ForeachRouter(Foreach owner, Publisher<CoreEvent> publisher, String expression, int batchSize,
                MessageProcessorChain nestedChain) {
    this.owner = owner;
    this.foreachContextResolver = new EventInternalContextResolver<>(MULE_FOREACH_CONTEXT_KEY,
                                                                     HashMap::new);

    upstreamFlux = from(publisher)
        .doOnNext(event -> {

          if (owner.validateExpression(event)) {
            downstreamRecorder.next(left(new IllegalArgumentException(MAP_NOT_SUPPORTED_MESSAGE)));
          }

          final CoreEvent responseEvent = prepareEvent(event, expression);
          inflightEvents.getAndIncrement();
          // Inject it into the inner flux
          innerRecorder.next(responseEvent);

        })
        .doOnComplete(() -> {
          //TODO MULE-18170
          if (inflightEvents.get() == 0) {
            completeRouter();
          } else {
            completeDeferred.set(true);
          }
        });

    innerFlux = create(innerRecorder)
        .map(event -> {
          ForeachContext foreachContext =
              foreachContextResolver.getCurrentContextFromEvent(event).get(event.getContext().getId());

          Iterator<TypedValue<?>> iterator = foreachContext.getIterator();
          if (!iterator.hasNext() && foreachContext.getElementNumber().get() == 0) {
            downstreamRecorder.next(right(Throwable.class, event));
            completeRouterIfNecessary();
          }

          TypedValue currentValue = owner.setCurrentValue(batchSize, foreachContext);
          return createTypedValuePartToProcess(owner, event, foreachContext, currentValue);

        })
        .transform(innerPub -> applyWithChildContext(innerPub, nestedChain, of(owner.getLocation())))
        .doOnNext(evt -> {
          try {
            ForeachContext foreachContext = foreachContextResolver.getCurrentContextFromEvent(evt).get(evt.getContext().getId());

            if (foreachContext.getOnComplete().isPresent()) {
              foreachContext.getOnComplete().get().run();
            }
            // Check if I have more to iterate:
            if (foreachContext.getIterator().hasNext()) {
              // YES - Inject again inside innerFlux. The Iterator automatically keeps track of the following elements
              innerRecorder.next(evt);
            } else {
              // NO - Propagate the first inside event down to downstreamFlux
              downstreamRecorder.next(right(evt));
              completeRouterIfNecessary();
            }
          } catch (Exception e) {
            // Delete foreach context
            this.eventWithCurrentContextDeleted(evt);
            downstreamRecorder.next(left(new MessagingException(evt, e, owner)));
            completeRouterIfNecessary();
          }
        }).onErrorContinue(MessagingException.class, (e, o) -> {
          this.eventWithCurrentContextDeleted(((MessagingException) e).getEvent());
          downstreamRecorder.next(left(e));
          completeRouterIfNecessary();
        });

    downstreamFlux = Flux.<Either<Throwable, CoreEvent>>create(sink -> {
      downstreamRecorder.accept(sink);
      // This will always run after the `downstreamCtxReference` is set
      subscribeUpstreamChains(downstreamCtxReference.get());
    })
        .doOnNext(event -> inflightEvents.decrementAndGet())
        .map(either -> {
          if (either.isLeft()) {
            throw propagate(either.getLeft());
          } else {
            // Create response and restore variables
            return createResponseEvent(either.getRight());
          }
        });
  }

  /**
   * If there are no events in-flight and the upstream publisher has received a completion signal, complete downstream publishers.
   */
  private void completeRouterIfNecessary() {
    if (completeDeferred.get() && inflightEvents.get() == 0) {
      completeRouter();
    }
  }

  private void completeRouter() {
    innerRecorder.complete();
    downstreamRecorder.complete();
  }

  private CoreEvent eventWithCurrentContextDeleted(CoreEvent event) {
    Map<String, ForeachContext> foreachContextContainer = foreachContextResolver.getCurrentContextFromEvent(event);
    foreachContextContainer.remove(event.getContext().getId());
    return foreachContextResolver.eventWithContext(event, foreachContextContainer);
  }

  private CoreEvent prepareEvent(CoreEvent event, String expression) {
    CoreEvent responseEvent =
        builder(event).addVariable(owner.getRootMessageVariableName(), event.getMessage(), MULE_MESSAGE).build();

    try {
      Iterator<TypedValue<?>> typedValueIterator = owner.splitRequest(responseEvent, expression);

      // Create ForEachContext
      ForeachContext foreachContext = this.createForeachContext(event, typedValueIterator);

      // TODO MULE-18174
      // Save it inside the internalParameters of the event
      Map<String, ForeachContext> currentContextFromEvent = foreachContextResolver.getCurrentContextFromEvent(event);
      currentContextFromEvent.put(event.getContext().getId(), foreachContext);

      responseEvent = foreachContextResolver.eventWithContext(responseEvent, currentContextFromEvent);

    } catch (Exception e) {
      // Delete foreach context
      eventWithCurrentContextDeleted(responseEvent);
      // Wrap any exception that occurs during split in a MessagingException. This is required as the
      // automatic wrapping is only applied when the signal is an Event.
      downstreamRecorder.next(left(new MessagingException(responseEvent, e, owner)));
      completeRouterIfNecessary();
    }
    return responseEvent;
  }

  private CoreEvent createTypedValuePartToProcess(Foreach owner, CoreEvent event, ForeachContext foreachContext,
                                                  TypedValue currentValue) {
    Optional<ItemSequenceInfo> itemSequenceInfo = of(ItemSequenceInfo.of(foreachContext.getElementNumber().get()));
    // For each TypedValue part process the nested chain using the event from the previous part.
    CoreEvent.Builder partEventBuilder = CoreEvent.builder(event).itemSequenceInfo(itemSequenceInfo);
    if (currentValue.getValue() instanceof EventBuilderConfigurer) {
      // Support EventBuilderConfigurer currently used by Batch Module
      EventBuilderConfigurer configurer = (EventBuilderConfigurer) currentValue.getValue();
      configurer.configure(partEventBuilder);

      Runnable onCompleteConsumer = configurer::eventCompleted;
      foreachContext.setOnComplete(onCompleteConsumer);

    } else if (currentValue.getValue() instanceof Message) {
      // If value is a Message then use it directly conserving attributes and properties.
      partEventBuilder.message((Message) currentValue.getValue());
    } else {
      // Otherwise create a new message
      partEventBuilder.message(Message.builder().payload(currentValue).build());
    }
    return partEventBuilder
        .addVariable(owner.getCounterVariableName(), foreachContext.getElementNumber().incrementAndGet(), NUMBER)
        .build();
  }

  private ForeachContext createForeachContext(CoreEvent event, Iterator<TypedValue<?>> iterator) {
    // Keep reference to existing rootMessage/count variables in order to restore later to support foreach nesting.
    Object previousCounterVar = event.getVariables().containsKey(owner.getCounterVariableName())
        ? event.getVariables().get(owner.getCounterVariableName()).getValue()
        : null;
    Object previousRootMessageVar =
        event.getVariables().containsKey(owner.getRootMessageVariableName())
            ? event.getVariables().get(owner.getRootMessageVariableName()).getValue()
            : null;

    return new ForeachContext(previousCounterVar, previousRootMessageVar, event.getMessage(), event.getItemSequenceInfo(),
                              iterator);
  }

  /**
   * Assembles and returns the downstream {@link Publisher <CoreEvent>}.
   *
   * @return the successful {@link CoreEvent} or retries exhaustion errors {@link Publisher}
   */
  Publisher<CoreEvent> getDownstreamPublisher() {
    return downstreamFlux
        .compose(downstreamPublisher -> subscriberContext()
            .flatMapMany(downstreamContext -> downstreamPublisher.doOnSubscribe(s -> {
              // When a transaction is active, the processing strategy executes the whole reactor chain in the same thread that
              // performs the subscription itself. Because of this, the subscription has to be deferred until the
              // downstreamPublisher FluxCreate#subscribe method registers the new sink in the recorder.
              downstreamCtxReference.set(downstreamContext);
            })));
  }

  private void subscribeUpstreamChains(Context downstreamContext) {
    innerFlux.subscriberContext(downstreamContext)
        .doOnError(e -> LOGGER.error("Exception during foreach execution: " + e.getCause()))
        .subscribe();
    upstreamFlux.subscriberContext(downstreamContext).subscribe();
  }

  private CoreEvent createResponseEvent(CoreEvent event) {
    ForeachContext foreachContext = foreachContextResolver.getCurrentContextFromEvent(event).get(event.getContext().getId());

    final CoreEvent.Builder responseBuilder =
        builder(event).message(foreachContext.getOriginalMessage()).itemSequenceInfo(foreachContext.getItemSequenceInfo());
    restoreVariables(foreachContext.getPreviousCounter(), foreachContext.getPreviousRootMessage(), responseBuilder);

    return eventWithCurrentContextDeleted(responseBuilder.build());
  }

  private void restoreVariables(Object previousCounterVar, Object previousRootMessageVar, CoreEvent.Builder responseBuilder) {
    // Restore original rootMessage/count variables.
    if (previousCounterVar != null) {
      responseBuilder.addVariable(owner.getCounterVariableName(), previousCounterVar, NUMBER);
    } else {
      responseBuilder.removeVariable(owner.getCounterVariableName());
    }
    if (previousRootMessageVar != null) {
      responseBuilder.addVariable(owner.getRootMessageVariableName(), previousRootMessageVar, MULE_MESSAGE);
    } else {
      responseBuilder.removeVariable(owner.getRootMessageVariableName());
    }
  }

}
