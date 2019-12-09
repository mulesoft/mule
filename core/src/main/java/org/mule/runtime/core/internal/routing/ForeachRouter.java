/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import com.google.common.collect.Iterators;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.BaseExceptionHandler;
import org.mule.runtime.core.internal.event.EventInternalContextResolver;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurer;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerIterator;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerList;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.util.context.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Optional.of;
import static org.mule.runtime.api.functional.Either.left;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.metadata.DataType.fromObject;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.internal.routing.ExpressionSplittingStrategy.DEFAULT_SPLIT_EXPRESSION;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Mono.subscriberContext;
import static reactor.util.context.Context.empty;

class ForeachRouter {

    private final Logger LOGGER = getLogger(ForeachRouter.class);

    private static final String FOREACH_CONTEXT_VARIABLE = "foreachContext";
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


    ForeachRouter(Foreach owner, Publisher<CoreEvent> publisher, String expression, int batchSize,
                  MessageProcessorChain nestedChain) {
        this.owner = owner;
        this.foreachContextResolver = new EventInternalContextResolver<>(FOREACH_CONTEXT_VARIABLE,
                HashMap::new);

        upstreamFlux = Flux.from(publisher)
                .doOnNext(event -> {
                    if (expression.equals(DEFAULT_SPLIT_EXPRESSION)
                            && Map.class.isAssignableFrom(event.getMessage().getPayload().getDataType().getType())) {
                        downstreamRecorder.next(left(new IllegalArgumentException(MAP_NOT_SUPPORTED_MESSAGE)));
                    }

                    // Keep reference to existing rootMessage/count variables in order to restore later to support foreach nesting.
                    Object previousCounterVar2 = event.getVariables().containsKey(owner.getCounterVariableName())
                            ? event.getVariables().get(owner.getCounterVariableName()).getValue()
                            : null;
                    Object previousRootMessageVar2 =
                            event.getVariables().containsKey(owner.getRootMessageVariableName())
                                    ? event.getVariables().get(owner.getRootMessageVariableName()).getValue()
                                    : null;

                    // Create ForEachContext
                    ForeachContext foreachContext =
                            new ForeachContext(previousCounterVar2, previousRootMessageVar2, event.getMessage());
                    // Save it inside the internalParameters of the event
                    Map<String, ForeachContext> currentContextFromEvent = foreachContextResolver.getCurrentContextFromEvent(event);
                    currentContextFromEvent.put(event.getContext().getId(), foreachContext);

                    final CoreEvent requestEvent =
                            builder(event).addVariable(owner.getRootMessageVariableName(), event.getMessage()).build();

                    CoreEvent coreEvent = foreachContextResolver.eventWithContext(requestEvent, currentContextFromEvent);
                    try {
                        // Set the Iterator<TypedValue> calculated
                        foreachContext.setIterator(splitRequest(coreEvent, expression));

                        // Inject it into the inner flux
                        innerRecorder.next(coreEvent);
                    } catch (Exception e) {
                        // Wrap any exception that occurs during split in a MessagingException. This is required as the
                        // automatic wrapping is only applied when the signal is an Event.
                        downstreamRecorder.next(left(new MessagingException(coreEvent, e, owner)));
                    }
                });

        innerFlux = Flux.create(innerRecorder)
                .map(event -> {

                    Iterator<TypedValue<?>> iterator = foreachContextResolver.getCurrentContextFromEvent(event).get(event.getContext().getId()).getIterator();
                    if (!iterator.hasNext() && foreachContextResolver.getCurrentContextFromEvent(event).get(event.getContext().getId()).getCount().get() == 0) {
                        downstreamRecorder.next(Either.right(event));
                    }

                    TypedValue currentValue;
                    if (batchSize > 1) {
                        int counter = 0;
                        ArrayList<Object> list = new ArrayList<>();
                        while (iterator.hasNext() && counter < batchSize) {
                            list.add(iterator.next());
                            counter++;
                        }
                        currentValue = new TypedValue<>(list, fromObject(list));
                    } else {
                        currentValue = iterator.next();
                    }

                    // For each TypedValue part process the nested chain using the event from the previous part.
                    CoreEvent.Builder partEventBuilder = CoreEvent.builder(event);
                    if (currentValue.getValue() instanceof EventBuilderConfigurer) {
                        // Support EventBuilderConfigurer currently used by Batch Module
                        EventBuilderConfigurer configurer = (EventBuilderConfigurer) currentValue.getValue();
                        configurer.configure(partEventBuilder);

                        Runnable onCompleteConsumer = configurer::eventCompleted;
                        foreachContextResolver.getCurrentContextFromEvent(event).get(event.getContext().getId()).setOnComplete(onCompleteConsumer);

                    } else if (currentValue.getValue() instanceof Message) {
                        // If value is a Message then use it directly conserving attributes and properties.
                        partEventBuilder.message((Message) currentValue.getValue());
                    } else {
                        // Otherwise create a new message
                        partEventBuilder.message(Message.builder().payload(currentValue).build());
                    }

                    // Create a mapper in assembly that that buffers the events, by taking from the iterable;
                    // or takes one from it and starts processing
                    return partEventBuilder
                            .addVariable(owner.getCounterVariableName(), foreachContextResolver.getCurrentContextFromEvent(event).get(event.getContext().getId()).getCount().incrementAndGet())
                            .build();
                })
                .transform(innerPub -> applyWithChildContext(innerPub, nestedChain, of(owner.getLocation()), chainErrorHandler()))
                .doOnNext(evt -> {
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
                    }
                }).onErrorContinue((e, o) -> downstreamRecorder.next(left(e)));

        downstreamFlux = Flux.<Either<Throwable, CoreEvent>>create(sink -> {
            downstreamRecorder.accept(sink);
            // This will always run after the `downstreamCtxReference` is set
            subscribeUpstreamChains(downstreamCtxReference.get());
        })
                .map(either -> {
                    if (either.isLeft()) {
                        throw propagate(either.getLeft());
                    } else {
                        // Create response and restore variables
                        return createResponseEvent(either.getRight());
                    }
                })
                .onErrorMap(MessagingException.class, me -> {
                    me.setProcessedEvent(createResponseEvent(me.getEvent()));
                    return me;
                });
    }

    private BaseExceptionHandler chainErrorHandler() {
        return new BaseExceptionHandler() {

            @Override
            public void onError(Exception exception) {
                eventWithCurrentContextDeleted(((MessagingException) exception).getEvent());
            }
        };
    }

    private CoreEvent eventWithCurrentContextDeleted(CoreEvent event) {
        Map<String, ForeachContext> foreachContextContainer = foreachContextResolver.getCurrentContextFromEvent(event);
        foreachContextContainer.remove(event.getContext().getId());
        return foreachContextResolver.eventWithContext(event, foreachContextContainer);
    }

    private Iterator<TypedValue<?>> splitRequest(CoreEvent request, String expression) {
        Object payloadValue = request.getMessage().getPayload().getValue();
        Iterator<TypedValue<?>> result;
        if (DEFAULT_SPLIT_EXPRESSION.equals(expression) && payloadValue instanceof EventBuilderConfigurerList) {
            // Support EventBuilderConfigurerList currently used by Batch Module
            result = Iterators.transform(((EventBuilderConfigurerList<Object>) payloadValue).eventBuilderConfigurerIterator(),
                    TypedValue::of);
        } else if (DEFAULT_SPLIT_EXPRESSION.equals(expression) && payloadValue instanceof EventBuilderConfigurerIterator) {
            // Support EventBuilderConfigurerIterator currently used by Batch Module
            result = new EventBuilderConfigurerIteratorWrapper((EventBuilderConfigurerIterator) payloadValue);
        } else {
            result = owner.getSplittingStrategy().split(request);
        }
        if (LOGGER.isDebugEnabled() && !result.hasNext()) {
            LOGGER.debug(
                    "<foreach> expression \"{}\" returned no results. If this is not expected please check your expression",
                    expression);
        }
        return result;
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
        innerFlux.subscriberContext(downstreamContext).subscribe();
        upstreamFlux.subscriberContext(downstreamContext).subscribe();
    }

    private CoreEvent createResponseEvent(CoreEvent event) {
        ForeachContext foreachContext = foreachContextResolver.getCurrentContextFromEvent(event).get(event.getContext().getId());
        final CoreEvent.Builder responseBuilder = builder(event).message(foreachContext.getOriginalMessage());
        restoreVariables(foreachContext.getPreviousCounter(), foreachContext.getPreviousRootMessage(), responseBuilder);
        return eventWithCurrentContextDeleted(responseBuilder.build());
    }

    private void restoreVariables(Object previousCounterVar, Object previousRootMessageVar, CoreEvent.Builder responseBuilder) {
        // Restore original rootMessage/count variables.
        if (previousCounterVar != null) {
            responseBuilder.addVariable(owner.getCounterVariableName(), previousCounterVar);
        } else {
            responseBuilder.removeVariable(owner.getCounterVariableName());
        }
        if (previousRootMessageVar != null) {
            responseBuilder.addVariable(owner.getRootMessageVariableName(), previousRootMessageVar);
        } else {
            responseBuilder.removeVariable(owner.getRootMessageVariableName());
        }
    }

    private static class EventBuilderConfigurerIteratorWrapper implements Iterator<TypedValue<?>> {

        private final EventBuilderConfigurerIterator configurerIterator;

        EventBuilderConfigurerIteratorWrapper(EventBuilderConfigurerIterator configurerIterator) {
            this.configurerIterator = configurerIterator;
        }

        @Override
        public boolean hasNext() {
            return configurerIterator.hasNext();
        }

        @Override
        public TypedValue<?> next() {
            return TypedValue.of(configurerIterator.nextEventBuilderConfigurer());
        }
    }

}
