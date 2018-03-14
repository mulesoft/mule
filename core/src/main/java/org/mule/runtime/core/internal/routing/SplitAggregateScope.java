/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.routing;

import static java.lang.Integer.MAX_VALUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.noEndpointsForRouter;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.component.ComponentUtils.getFromAnnotatedObject;
import static org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory.DIRECT_PROCESSING_STRATEGY_INSTANCE;
import static org.mule.runtime.core.internal.routing.ExpressionSplittingStrategy.DEFAULT_SPLIT_EXPRESSION;
import static org.mule.runtime.core.internal.routing.FirstSuccessfulRoutingStrategy.validateMessageIsNotConsumable;
import static org.mule.runtime.core.internal.routing.ForkJoinStrategy.RoutingPair.of;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.getProcessingStrategy;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static reactor.core.publisher.Flux.fromIterable;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.internal.routing.forkjoin.CollectListForkJoinStrategyFactory;
import org.mule.runtime.core.internal.routing.forkjoin.CollectMapForkJoinStrategyFactory;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import org.reactivestreams.Publisher;

/**
 * <p>
 * The <code>Split-Aggregate</code> scope splits the incoming {@link org.mule.runtime.api.message.Message} into n parts will
 * broadcast copies of the current message to every route in parallel subject to any limitation in concurrency that has been
 * configured
 * <p>
 * For advanced use cases, a custom {@link ForkJoinStrategyFactory} can be applied to customize the logic used to aggregate the
 * route responses back into one single Event.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/BroadcastAggregate.html"<a/>
 * </p>
 *
 * @since 4.2.0
 */
public class SplitAggregateScope extends AbstractForkJoinRouter {

  private String collectionExpression = DEFAULT_SPLIT_EXPRESSION;
  private SplittingStrategy<CoreEvent, Iterator<TypedValue<?>>> splittingStrategy;

  private List<Processor> messageProcessors;
  private MessageProcessorChain nestedChain;

  @Override
  public void initialise() throws InitialisationException {
    nestedChain = newChain(Optional.of(resolveProcessingStrategy()), messageProcessors);
    nestedChain.setMuleContext(muleContext);
    splittingStrategy = new ExpressionSplittingStrategy(muleContext.getExpressionManager(), collectionExpression);
    super.initialise();
  }

  @Override
  protected Publisher<ForkJoinStrategy.RoutingPair> getRoutingPairs(CoreEvent event) {
    return fromIterable(() -> splittingStrategy.split(event))
        .map(partTypedValue -> CoreEvent.builder(event).message(Message.builder().payload(partTypedValue).build()).build())
        .map(partEvent -> of(partEvent, nestedChain));
  }

  @Override
  protected List<MessageProcessorChain> getOwnedObjects() {
    return singletonList(nestedChain);
  }

  public void setMessageProcessors(List<Processor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  @Override
  protected boolean isDelayErrors() {
    return true;
  }

  @Override
  protected int getDefaultMaxConcurrency() {
    return MAX_VALUE;
  }

  @Override
  protected ForkJoinStrategyFactory getDefaultForkJoinStrategyFactory() {
    return new CollectListForkJoinStrategyFactory();
  }

  /**
   * Set the expression used to split the incoming message.
   * 
   * @param collectionExpression
   */
  public void setCollectionExpression(String collectionExpression) {
    this.collectionExpression = collectionExpression;
  }
}
