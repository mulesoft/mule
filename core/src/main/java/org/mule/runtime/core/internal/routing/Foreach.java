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
import static org.mule.runtime.core.api.InternalEvent.builder;
import static org.mule.runtime.core.api.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.api.processor.MessageProcessors.processToApply;
import static org.mule.runtime.core.internal.routing.ExpressionSplittingStrategy.DEFAULT_SPIT_EXPRESSION;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.fromIterable;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.InternalEvent.Builder;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.Scope;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.util.collection.SplittingStrategy;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurer;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerIterator;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerList;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.google.common.collect.Iterators;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
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
 * The {@link InternalEvent} sent to the next message processor is the same that arrived to foreach.
 */
public class Foreach extends AbstractMessageProcessorOwner implements Initialisable, Scope {

  private static final String DEFAULT_ROOT_MESSAGE_PROPERTY = "rootMessage";
  private static final Logger LOGGER = getLogger(Foreach.class);

  private List<Processor> messageProcessors;
  private String expression = DEFAULT_SPIT_EXPRESSION;
  private int batchSize = 1;
  private SplittingStrategy<InternalEvent, Iterator<TypedValue<?>>> splittingStrategy;
  private String rootMessageVariableName = DEFAULT_ROOT_MESSAGE_PROPERTY;
  private String counterVariableName;
  private MessageProcessorChain nestedChain;

  @Override
  public InternalEvent process(InternalEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<InternalEvent> apply(Publisher<InternalEvent> publisher) {
    return from(publisher).flatMap(originalEvent -> {

      // Keep reference to existing rootMessage/count variables in order to restore later to support foreach nesting.
      final Object previousCounterVar = originalEvent.getVariables().containsKey(counterVariableName)
          ? originalEvent.getVariables().get(counterVariableName).getValue()
          : null;
      final Object previousRootMessageVar =
          originalEvent.getVariables().containsKey(rootMessageVariableName)
              ? originalEvent.getVariables().get(rootMessageVariableName).getValue()
              : null;

      final InternalEvent requestEvent =
          builder(originalEvent).addVariable(rootMessageVariableName, originalEvent.getMessage()).build();

      return splitAndProcess(requestEvent)
          .map(result -> {
            final Builder responseBuilder = builder(result).message(originalEvent.getMessage());
            restoreVariables(previousCounterVar, previousRootMessageVar, responseBuilder);
            return responseBuilder.build();
          })
          .onErrorMap(MessagingException.class, me -> {
            // Restore variables in case of error also
            InternalEvent.Builder exceptionEventBuilder = builder(me.getEvent());
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

  private Flux<InternalEvent> splitAndProcess(InternalEvent request) {
    AtomicInteger count = new AtomicInteger();
    final AtomicReference<InternalEvent> currentEvent = new AtomicReference<>(request);
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
                                  .transform(nestedChain)
                                  .doOnNext(result -> currentEvent.set(InternalEvent.builder(result).build()));
                            })
                            .takeLast(1)
                            .map(s -> InternalEvent.builder(currentEvent.get()).message(request.getMessage()).build());
  }

  private Iterator<TypedValue<?>> splitRequest(InternalEvent request) {
    Object payloadValue = request.getMessage().getPayload().getValue();
    if (DEFAULT_SPIT_EXPRESSION.equals(expression) && payloadValue instanceof EventBuilderConfigurerList) {
      // Support EventBuilderConfigurerList currently used by Batch Module
      return Iterators.transform(((EventBuilderConfigurerList<Object>) payloadValue).eventBuilderConfigurerIterator(),
                                 input -> TypedValue.of(input));
    } else if (DEFAULT_SPIT_EXPRESSION.equals(expression) && payloadValue instanceof EventBuilderConfigurerIterator) {
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
