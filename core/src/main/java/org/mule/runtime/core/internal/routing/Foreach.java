/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.mule.runtime.api.metadata.DataType.fromObject;
import static org.mule.runtime.core.internal.routing.ExpressionSplittingStrategy.DEFAULT_SPLIT_EXPRESSION;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.buildNewChainWithListOfProcessors;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.collect.Iterators;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerIterator;
import org.mule.runtime.core.internal.routing.outbound.EventBuilderConfigurerList;
import org.mule.runtime.core.privileged.processor.Scope;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;

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

  private static final Logger LOGGER = getLogger(Foreach.class);

  static final String DEFAULT_COUNTER_VARIABLE = "counter";
  public static final String DEFAULT_ROOT_MESSAGE_VARIABLE = "rootMessage";
  private String counterVariableName = DEFAULT_COUNTER_VARIABLE;
  private String rootMessageVariableName = DEFAULT_ROOT_MESSAGE_VARIABLE;

  @Inject
  protected ExpressionManager expressionManager;

  private List<Processor> messageProcessors;
  private String expression = DEFAULT_SPLIT_EXPRESSION;
  private int batchSize = 1;
  private SplittingStrategy<CoreEvent, Iterator<TypedValue<?>>> splittingStrategy;

  private MessageProcessorChain nestedChain;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return processToApply(event, this);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return new ForeachRouter(this, publisher, expression, batchSize, nestedChain).getDownstreamPublisher();
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(nestedChain);
  }

  public void setMessageProcessors(List<Processor> messageProcessors) {
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

  public String getRootMessageVariableName() {
    return rootMessageVariableName;
  }

  public void setRootMessageVariableName(String rootMessageVariableName) {
    this.rootMessageVariableName = rootMessageVariableName;
  }

  public String getCounterVariableName() {
    return counterVariableName;
  }

  public void setCounterVariableName(String counterVariableName) {
    this.counterVariableName = counterVariableName;
  }

  public SplittingStrategy<CoreEvent, Iterator<TypedValue<?>>> getSplittingStrategy() {
    return splittingStrategy;
  }

  boolean validateExpression(CoreEvent event) {
    return expression.equals(DEFAULT_SPLIT_EXPRESSION)
        && Map.class.isAssignableFrom(event.getMessage().getPayload().getDataType().getType());
  }

  Iterator<TypedValue<?>> splitRequest(CoreEvent request, String expression) {
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
      result = getSplittingStrategy().split(request);
    }
    if (LOGGER.isDebugEnabled() && !result.hasNext()) {
      LOGGER.debug(
                   "<foreach> expression \"{}\" returned no results. If this is not expected please check your expression",
                   expression);
    }
    return result;
  }

  TypedValue setCurrentValue(int batchSize, ForeachContext foreachContext) {
    TypedValue currentValue;
    Iterator<TypedValue<?>> iterator = foreachContext.getIterator();
    if (batchSize > 1) {
      int counter = 0;
      List currentBatch = new ArrayList<>();
      while (iterator.hasNext() && counter < batchSize) {
        currentBatch.add(iterator.next());
        counter++;
      }

      if (!foreachContext.getBatchDataType().isPresent()) {
        foreachContext.setBatchDataType(of(fromObject(currentBatch)));
      }
      currentValue = new TypedValue<>(currentBatch, foreachContext.getBatchDataType().get());
    } else {
      currentValue = iterator.next();
    }
    return currentValue;
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
