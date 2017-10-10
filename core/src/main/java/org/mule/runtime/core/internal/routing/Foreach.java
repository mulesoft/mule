/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.core.api.event.CoreEvent.builder;
import static org.mule.runtime.core.internal.routing.ExpressionSplittingStrategy.DEFAULT_SPLIT_EXPRESSION;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.defer;
import static reactor.core.publisher.Mono.empty;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.event.CoreEvent.Builder;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurer;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerIterator;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerList;

import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * The {@code foreach} {@link Processor} allows iterating over a collection payload, or any collection obtained by an expression,
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
  static final String DEFAULT_COUNTER_VARIABLE = "counter";
  static final String MAP_NOT_SUPPORTED_MESSAGE =
      "Foreach does not support 'java.util.Map' with no collection expression. To iterate over Map entries use '#[dw::core::Objects::entrySet(payload)]'";

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
    return from(publisher)
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

          return splitAndProcess(requestEvent)
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
              })
              // Required due to lack of decent support for error-handling in reactor. See
              // https://github.com/reactor/reactor-core/issues/629.
              .onErrorMap(throwable -> !(throwable instanceof MessagingException),
                          throwable -> new MessagingException(originalEvent, throwable, this));
        });
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

  private Flux<CoreEvent> splitAndProcess(CoreEvent request) {
    AtomicInteger count = new AtomicInteger();
    final AtomicReference<CoreEvent> currentEvent = new AtomicReference<>(request);
    return fromIterable(
                        // Split into sequence of TypedValue
                        () -> splitRequest(request))
                            // If batchSize > 1 then buffer sequence into List<TypedValue<T>> and convert to TypedValue<List<T>>.
                            .transform(p -> batchSize > 1 ? from(p).buffer(batchSize).map(typedValueListToTypedValue()) : p)
                            // For each TypedValue part process the nested chain using the event from the previous part.
                            .concatMap(typedValue -> {
                              Builder partEventBuilder = builder(currentEvent.get());
                              if (typedValue.getValue() instanceof EventBuilderConfigurer) {
                                // Support EventBuilderConfigurer currently used by Batch Module
                                ((EventBuilderConfigurer) typedValue.getValue()).configure(partEventBuilder);
                              } else if (typedValue.getValue() instanceof Message) {
                                // If value is a Message then use it directly conserving attributes and properties.
                                partEventBuilder.message((Message) typedValue.getValue());
                              } else {
                                // Otherwise create a new message
                                partEventBuilder.message(Message.builder().payload(typedValue).build());
                              }
                              return just(partEventBuilder.addVariable(counterVariableName, count.incrementAndGet()).build())
                                  .transform(nestedChain).doOnNext(result -> currentEvent.set(CoreEvent.builder(result).build()));
                            })
                            // This can potentially be improved but simplest way currently to determine if split results in empty
                            // iterator is to check atomic count
                            .switchIfEmpty(defer(() -> {
                              if (count.get() == 0) {
                                logger
                                    .warn("Split expression returned no results. If this is not expected please check your expression");
                                return just(request);
                              } else {
                                return empty();
                              }
                            }))
                            .takeLast(1)
                            .map(s -> CoreEvent.builder(currentEvent.get()).message(request.getMessage()).build());
  }

  private Iterator<TypedValue<?>> splitRequest(CoreEvent request) {
    Object payloadValue = request.getMessage().getPayload().getValue();
    if (DEFAULT_SPLIT_EXPRESSION.equals(expression) && payloadValue instanceof EventBuilderConfigurerList) {
      // Support EventBuilderConfigurerList currently used by Batch Module
      return Iterators.transform(((EventBuilderConfigurerList<Object>) payloadValue).eventBuilderConfigurerIterator(),
                                 input -> TypedValue.of(input));
    } else if (DEFAULT_SPLIT_EXPRESSION.equals(expression) && payloadValue instanceof EventBuilderConfigurerIterator) {
      // Support EventBuilderConfigurerIterator currently used by Batch Module
      return new EventBuilderConfigurerIteratorWrapper((EventBuilderConfigurerIterator) payloadValue);
    } else {
      return splittingStrategy.split(request);
    }
  }

  /*
   * Convert List<TypedValue<Object>> resulting from applying batch size to TypedValue<List<Object>>
   */
  private Function<List<TypedValue<?>>, TypedValue<List>> typedValueListToTypedValue() {
    return list -> {
      DataType dataType = OBJECT;
      if (list.stream().map(i -> i.getDataType()).distinct().count() == 1) {
        dataType = list.stream().findFirst().get().getDataType();
      }
      return new TypedValue(list
          .stream()
          .map(tv -> tv.getValue())
          .collect(toList()),
                            DataType.builder()
                                .collectionType(List.class)
                                .itemType(dataType.getType())
                                .itemMediaType(dataType.getMediaType()).build());
    };
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
    Optional<ProcessingStrategy> processingStrategy = getProcessingStrategy(muleContext, getRootContainerName());
    nestedChain = newChain(processingStrategy, messageProcessors);
    splittingStrategy = new ExpressionSplittingStrategy(muleContext.getExpressionManager(), expression);
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
