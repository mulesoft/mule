/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonList;
import static java.util.Optional.*;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.metadata.DataType.fromObject;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.internal.routing.ExpressionSplittingStrategy.DEFAULT_SPLIT_EXPRESSION;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.applyWithChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChildContext;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processWithChildContext;
import static reactor.core.Exceptions.propagate;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurer;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerIterator;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerList;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import com.google.common.collect.Iterators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * The {@code foreach} {@link Processor} allow
 *
 *
 * s iterating over a collection payload, or any collection obtained by an expression,
 * generating a message for each element.
 * <p>
 * The number of the message being processed is stored in {@code #[mel:variable:counter]} and the root message is store in
 * {@code #[mel:flowVars.rootMessage]}. Both variables may be renamed by means of {@link #setCounterVariableName(String)} and
 * {@link #setRootMessageVariableName(String)}.
 * <p>
 * Defining a groupSize greater than one, allows iterating over collections of elements of the specified size.
 * <p>
 * The {@link CoreEvent} sent to the next message processor is the same that arrived to foreach.
 */
public class Foreach extends AbstractMessageProcessorOwner implements Initialisable, Scope {

  public static final String DEFAULT_ROOT_MESSAGE_VARIABLE = "rootMessage";
  public static final String FOREACH_CONTEXT_VARIABLE = "foreachContext";
  static final String DEFAULT_COUNTER_VARIABLE = "counter";
  static final String MAP_NOT_SUPPORTED_MESSAGE =
      "Foreach does not support 'java.util.Map' with no collection expression. To iterate over Map entries use '#[dw::core::Objects::entrySet(payload)]'";

  @Inject
  protected ExpressionManager expressionManager;

  private List<Processor> messageProcessors;
  private String expression = DEFAULT_SPLIT_EXPRESSION;
  private int batchSize = 1;
  private SplittingStrategy<CoreEvent, Iterator<TypedValue<?>>> splittingStrategy;
  private String rootMessageVariableName = DEFAULT_ROOT_MESSAGE_VARIABLE;
  private String counterVariableName = DEFAULT_COUNTER_VARIABLE;
  private MessageProcessorChain nestedChain;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {

    FluxSinkRecorder<CoreEvent> innerRecorder = new FluxSinkRecorder<>();
    FluxSinkRecorder<Either<Throwable, CoreEvent>> downstreamRecorder = new FluxSinkRecorder<>();

    Flux<CoreEvent> upstreamFlux = Flux.from(publisher)
        .doOnNext(event -> {
          if (expression.equals(DEFAULT_SPLIT_EXPRESSION)
              && Map.class.isAssignableFrom(event.getMessage().getPayload().getDataType().getType())) {
            throw new IllegalArgumentException(MAP_NOT_SUPPORTED_MESSAGE);
          }

          // Keep reference to existing rootMessage/count variables in order to restore later to support foreach nesting.
          Object previousCounterVar2 = event.getVariables().containsKey(counterVariableName)
              ? event.getVariables().get(counterVariableName).getValue()
              : null;
          Object previousRootMessageVar2 =
              event.getVariables().containsKey(rootMessageVariableName)
                  ? event.getVariables().get(rootMessageVariableName).getValue()
                  : null;

          // Create ForEachContext, with the Iterator<TypedValue> calculated
          ForeachContext foreachContext =
              new ForeachContext(previousCounterVar2, previousRootMessageVar2, event.getMessage(), splitRequest(event));

          // Save it inside the internalParameters of the event
          final CoreEvent requestEvent =
              builder(event).addVariable(rootMessageVariableName, event.getMessage())
                  .addVariable(FOREACH_CONTEXT_VARIABLE, foreachContext).build();

          // Inject it into the inner flux
          innerRecorder.next(requestEvent);
        });

    AtomicInteger count = new AtomicInteger();

    Flux<CoreEvent> innerFlux = Flux.create(innerRecorder)
        .map(event -> {

          Iterator<TypedValue<?>> iterator = getForeachContextFromEvent(event).getIterator();

          TypedValue currentValue;
          if (batchSize > 1) {
            int counter = 0;
            ArrayList<Object> list = new ArrayList<>();
            while (iterator.hasNext() && batchSize > 1 && counter < batchSize) {
              list.add(iterator.next());
              counter++;
            }
            currentValue = new TypedValue<>(list, fromObject(list));
          } else {
            currentValue = iterator.next();
          }

          // For each TypedValue part process the nested chain using the event from the previous part.
          BaseEventContext childContext = newChildContext(event, ofNullable(getLocation()));

          Builder partEventBuilder = CoreEvent.builder(event);
          if (currentValue.getValue() instanceof EventBuilderConfigurer) {
            // Support EventBuilderConfigurer currently used by Batch Module
            EventBuilderConfigurer configurer = (EventBuilderConfigurer) currentValue.getValue();
            configurer.configure(partEventBuilder);

            childContext.onResponse((e, t) -> configurer.eventCompleted());
          } else if (currentValue.getValue() instanceof Message) {
            // If value is a Message then use it directly conserving attributes and properties.
            partEventBuilder.message((Message) currentValue.getValue());
          } else {
            // Otherwise create a new message
            partEventBuilder.message(Message.builder().payload(currentValue).build());
          }

          // Create a mapper in assembly that that buffers the events, by taking from the iterable;
          // or takes one from it and starts processing
          return partEventBuilder.addVariable(counterVariableName, count.incrementAndGet()).build();
        })
        .transform(innerPub -> applyWithChildContext(innerPub, nestedChain, of(Foreach.this.getLocation())))
        .doOnNext(evt -> {
          ForeachContext foreachContext = getForeachContextFromEvent(evt);
          // Check if I have more to iterate:
          if (foreachContext.getIterator().hasNext()) {
            // YES - Inject again inside innerFlux. The Iterator automatically keeps track of the following elements
            innerRecorder.next(evt);
          } else {
            // NO - Propagate the first inside event down to downstreamFlux
            downstreamRecorder.next(right(evt));
          }
        });

    Flux<CoreEvent> downstreamFlux = Flux.create(downstreamRecorder)
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

    return upstreamFlux;
    /* return from(publisher)
        .doOnNext(event -> {
          if (expression.equals(DEFAULT_SPLIT_EXPRESSION)
              && Map.class.isAssignableFrom(event.getMessage().getPayload().getDataType().getType())) {
            throw new IllegalArgumentException(MAP_NOT_SUPPORTED_MESSAGE);
          }
        })
        .flatMap(originalEvent -> {
    
          // Keep reference to existing rootMessage/count variables in order to restore later to support foreach nesting.
          final Object previousCounterVar = originalEvent.getVariables().containsKey(counterVariableName)
              ? originalEvent.getVariables().get(counterVariableName).getValue()
              : null;
          final Object previousRootMessageVar =
              originalEvent.getVariables().containsKey(rootMessageVariableName)
                  ? originalEvent.getVariables().get(rootMessageVariableName).getValue()
                  : null;
    
          final CoreEvent requestEvent =
              builder(originalEvent).addVariable(rootMessageVariableName, originalEvent.getMessage()).build();
    
          return Mono.from(splitAndProcess(requestEvent))
              .map(result -> {
                final Builder responseBuilder = builder(result).message(originalEvent.getMessage());
                restoreVariables(previousCounterVar, previousRootMessageVar, responseBuilder);
                return responseBuilder.build();
              })
              .onErrorMap(MessagingException.class, me -> {
                // Restore variables in case of error also
                CoreEvent.Builder exceptionEventBuilder = builder(me.getEvent());
                restoreVariables(previousCounterVar, previousRootMessageVar, exceptionEventBuilder);
                me.setProcessedEvent(exceptionEventBuilder.build());
                return me;
              });
        });*/
  }

  private CoreEvent createResponseEvent(CoreEvent event) {
    ForeachContext foreachContext = getForeachContextFromEvent(event);
    final Builder responseBuilder = builder(event).message(foreachContext.getOriginalMessage());
    restoreVariables(foreachContext.getPreviousCounter(), foreachContext.getPreviousRootMessage(), responseBuilder);
    return responseBuilder.build();
  }

  private ForeachContext getForeachContextFromEvent(CoreEvent event) {
    return (ForeachContext) event.getVariables().get(FOREACH_CONTEXT_VARIABLE).getValue();
  }

  private void restoreVariables(Object previousCounterVar, Object previousRootMessageVar, Builder responseBuilder) {
    // Restore original rootMessage/count variables.
    if (previousCounterVar != null) {
      responseBuilder.addVariable(counterVariableName, previousCounterVar);
    } else {
      responseBuilder.removeVariable(counterVariableName);
    }
    if (previousRootMessageVar != null) {
      responseBuilder.addVariable(rootMessageVariableName, previousRootMessageVar);
    } else {
      responseBuilder.removeVariable(rootMessageVariableName);
    }
  }

  private Publisher<CoreEvent> splitAndProcess(CoreEvent request) {
    AtomicInteger count = new AtomicInteger();
    final AtomicReference<CoreEvent> currentEvent = new AtomicReference<>(request);

    // Split into sequence of TypedValue
    return fromIterable(() -> splitRequest(request))
        // If batchSize > 1 then buffer sequence into List<TypedValue<T>> and convert to
        // TypedValue<List<TypedValue<T>>>.
        .transform(p -> batchSize > 1
            ? from(p).buffer(batchSize).map(list -> new TypedValue<>(list, fromObject(list)))
            : p)
        // For each TypedValue part process the nested chain using the event from the previous part.
        .flatMapSequential(typedValue -> {
          BaseEventContext childContext = newChildContext(currentEvent.get(), ofNullable(getLocation()));

          Builder partEventBuilder = builder(currentEvent.get());
          if (typedValue.getValue() instanceof EventBuilderConfigurer) {
            // Support EventBuilderConfigurer currently used by Batch Module
            EventBuilderConfigurer configurer = (EventBuilderConfigurer) typedValue.getValue();
            configurer.configure(partEventBuilder);

            childContext.onResponse((e, t) -> {
              configurer.eventCompleted();
            });
          } else if (typedValue.getValue() instanceof Message) {
            // If value is a Message then use it directly conserving attributes and properties.
            partEventBuilder.message((Message) typedValue.getValue());
          } else {
            // Otherwise create a new message
            partEventBuilder.message(Message.builder().payload(typedValue).build());
          }

          return Mono
              .from(processWithChildContext(partEventBuilder.addVariable(counterVariableName, count.incrementAndGet()).build(),
                                            nestedChain, childContext))
              .doOnNext(result -> currentEvent.set(CoreEvent.builder(result).build()));
        },
                           // Force sequential execution of the chain for each element
                           1)
        // This can potentially be improved but simplest way currently to determine if split results in empty
        // iterator is to check atomic count
        .switchIfEmpty(defer(() -> {
          if (count.get() == 0) {
            if (logger.isDebugEnabled()) {
              logger.debug(
                           "<foreach> expression \"{}\" returned no results. If this is not expected please check your expression",
                           expression);
            }
            return just(request);
          } else {
            return empty();
          }
        }))
        .takeLast(1)
        .map(s -> CoreEvent.builder(currentEvent.get()).message(request.getMessage()).build())
        .onErrorStop();
  }

  private Iterator<TypedValue<?>> splitRequest(CoreEvent request) {
    try {
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
        result = splittingStrategy.split(request);
      }
      if (logger.isDebugEnabled() && !result.hasNext()) {
        logger.debug(
                     "<foreach> expression \"{}\" returned no results. If this is not expected please check your expression",
                     expression);
      }
      return result;
    } catch (Exception e) {
      // Wrap any exception that occurs during split in a MessagingException. This is required as the
      // automatic wrapping is only applied when the signal is an Event.
      throw propagate(new MessagingException(request, e, Foreach.this));
    }
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(nestedChain);
  }

  public void setMessageProcessors(List<Processor> messageProcessors) throws MuleException {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public void initialise() throws InitialisationException {
    Optional<ProcessingStrategy> processingStrategy = getProcessingStrategy(locator, getRootContainerLocation());
    nestedChain = buildNewChainWithListOfProcessors(processingStrategy, messageProcessors);
    splittingStrategy = new ExpressionSplittingStrategy(expressionManager, expression);
    super.initialise();
  }

  public void setCollectionExpression(String expression) {
    this.expression = expression;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public void setRootMessageVariableName(String rootMessageVariableName) {
    this.rootMessageVariableName = rootMessageVariableName;
  }

  public void setCounterVariableName(String counterVariableName) {
    this.counterVariableName = counterVariableName;
  }

  private static class EventBuilderConfigurerIteratorWrapper implements Iterator<TypedValue<?>> {

    private final EventBuilderConfigurerIterator configurerIterator;

    public EventBuilderConfigurerIteratorWrapper(EventBuilderConfigurerIterator configurerIterator) {
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
