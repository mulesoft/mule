/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
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

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

  private static final String ROOT_MESSAGE_PROPERTY = "rootMessage";
  private static final Logger LOGGER = getLogger(Foreach.class);

  private List<Processor> messageProcessors;
  private String expression = DEFAULT_SPIT_EXPRESSION;
  private int batchSize = 1;
  private SplittingStrategy<InternalEvent, Iterator<TypedValue<?>>> splittingStrategy;
  private String rootMessageVariableName;
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
      final String parentMessageProp = rootMessageVariableName != null ? rootMessageVariableName : ROOT_MESSAGE_PROPERTY;
      final Object previousCounterVar = originalEvent.getVariables().containsKey(counterVariableName)
          ? originalEvent.getVariables().get(counterVariableName).getValue()
          : null;
      final Object previousRootMessageVar =
          originalEvent.getVariables().containsKey(parentMessageProp)
              ? originalEvent.getVariables().get(parentMessageProp).getValue()
              : null;

      final InternalEvent requestEvent =
          builder(originalEvent).addVariable(parentMessageProp, originalEvent.getMessage()).build();
      return splitAndProcess(requestEvent)
          .map(result -> {
            final Builder responseBuilder = builder(result).message(originalEvent.getMessage());

            // Restore original rootMessage/count variables.
            if (previousCounterVar != null) {
              responseBuilder.addVariable(counterVariableName, previousCounterVar);
            } else {
              responseBuilder.removeVariable(counterVariableName);
            }
            if (previousRootMessageVar != null) {
              responseBuilder.addVariable(parentMessageProp, previousRootMessageVar);
            } else {
              responseBuilder.removeVariable(parentMessageProp);
            }
            return responseBuilder.build();
          })
          // Required due to lack of decent support for error-handling in reactor. See
          // https://github.com/reactor/reactor-core/issues/629.
          .onErrorMap(throwable -> !(throwable instanceof MessagingException),
                      throwable -> new MessagingException(originalEvent, throwable, this));
    });
  }

  private Flux<InternalEvent> splitAndProcess(InternalEvent request) {
    AtomicInteger count = new AtomicInteger();
    return fromIterable(() -> splittingStrategy.split(request))
        .transform(p -> batchSize > 1 ? from(p).buffer(batchSize).map(typedValueListToTypedValue()) : p)
        .reduce(just(request),
                (internalEventFlux, typedValue) -> Mono.from(internalEventFlux)
                    .map(e -> {
                      Builder partEventBuilder = builder(e);
                      if (typedValue.getValue() instanceof Message) {
                        partEventBuilder.message((Message) typedValue.getValue());
                      } else {
                        partEventBuilder.message(Message.builder().payload(typedValue).build());
                      }
                      return partEventBuilder.addVariable(counterVariableName, count.incrementAndGet()).build();
                    })
                    .transform(nestedChain))
        .flatMapMany(identity());
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

}
