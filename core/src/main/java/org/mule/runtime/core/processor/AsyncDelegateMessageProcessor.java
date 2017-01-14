/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.util.Collections.singletonList;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.api.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.rx.Exceptions.UNEXPECTED_EXCEPTION_PREDICATE;
import static org.mule.runtime.core.api.rx.Exceptions.checkedConsumer;
import static org.mule.runtime.core.api.rx.Exceptions.rxExceptionToMuleException;
import static org.mule.runtime.core.config.i18n.CoreMessages.asyncDoesNotSupportTransactions;
import static org.mule.runtime.core.config.i18n.CoreMessages.objectIsNull;
import static org.mule.runtime.core.transaction.TransactionCoordination.isTransactionActive;
import static org.mule.runtime.core.util.concurrent.ThreadNameHelper.getPrefix;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAware;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.strategy.LegacyAsynchronousProcessingStrategyFactory;
import org.mule.runtime.core.util.NotificationUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

/**
 * Processes {@link Event}'s asynchronously using a {@link ProcessingStrategy} to schedule asynchronous processing of
 * MessageProcessor delegate configured the next {@link Processor}. The next {@link Processor} is therefore be executed in a
 * different thread regardless of the exchange-pattern configured on the inbound endpoint. If a transaction is present then an
 * exception is thrown.
 */
public class AsyncDelegateMessageProcessor extends AbstractMessageProcessorOwner
    implements Processor, Initialisable, Startable, Stoppable, MessagingExceptionHandlerAware {

  protected Logger logger = LoggerFactory.getLogger(getClass());
  private AtomicBoolean consumablePayloadWarned = new AtomicBoolean(false);

  protected MessageProcessorChain delegate;

  protected ProcessingStrategyFactory processingStrategyFactory = new LegacyAsynchronousProcessingStrategyFactory();
  protected ProcessingStrategy processingStrategy;
  protected String name;
  private MessagingExceptionHandler messagingExceptionHandler;

  public AsyncDelegateMessageProcessor(MessageProcessorChain delegate) {
    this.delegate = delegate;
  }

  public AsyncDelegateMessageProcessor(MessageProcessorChain delegate,
                                       ProcessingStrategyFactory processingStrategyFactory,
                                       String name) {
    this.delegate = delegate;
    this.processingStrategyFactory = processingStrategyFactory;
    this.name = name;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (delegate == null) {
      throw new InitialisationException(objectIsNull("delegate message processor"), this);
    }
    if (processingStrategyFactory == null) {
      throw new InitialisationException(objectIsNull("processingStrategy"), this);
    }
    processingStrategy = processingStrategyFactory.create(muleContext, getPrefix(muleContext) + name);
    super.initialise();
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(processingStrategy);
    super.start();
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(processingStrategy);
    super.stop();
  }

  @Override
  public Event process(Event event) throws MuleException {
    try {
      return Mono.just(event).transform(this).block();
    } catch (Throwable e) {
      throw rxExceptionToMuleException(e);
    }
  }

  private void assertNotTransactional(Event event) throws RoutingException {
    if (isTransactionActive()) {
      throw new RoutingException(asyncDoesNotSupportTransactions(), delegate);
    }
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher)
        .doOnNext(checkedConsumer(event -> assertNotTransactional(event)))
        .doOnNext(event -> warnConsumablePayload(event.getMessage()))
        .doOnNext(request -> just(request)
            .map(event1 -> updateEventForAsync(event1))
            .transform(processingStrategy.onPipeline(flowConstruct, delegate, messagingExceptionHandler))
            .onErrorResumeWith(MessagingException.class, messagingExceptionHandler)
            .doOnError(UNEXPECTED_EXCEPTION_PREDICATE,
                       exception -> logger.error("Unhandled exception in async processing.", exception))
            .subscribe());
  }

  private Event updateEventForAsync(Event event) {
    // Clone event, make it async and remove ReplyToHandler
    Event newEvent = Event.builder(event).exchangePattern(ONE_WAY).replyToHandler(null).build();
    // Update RequestContext ThreadLocal for backwards compatibility
    setCurrentEvent(newEvent);
    return newEvent;
  }

  private void warnConsumablePayload(InternalMessage message) {
    if (consumablePayloadWarned.compareAndSet(false, true) && message.getPayload().getDataType().isStreamType()) {
      logger.warn(String.format("Using 'async' router with consumable payload (%s) may lead to unexpected results." +
          " Please ensure that only one of the branches actually consumes the payload, or transform it by using an <object-to-byte-array-transformer>.",
                                message.getPayload().getValue().getClass().getName()));
    }
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return singletonList(delegate);
  }

  public ProcessingStrategy getProcessingStrategy() {
    return processingStrategy;
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    NotificationUtils.addMessageProcessorPathElements(delegate, pathElement.addChild(this));
  }

  @Override
  public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler) {
    this.messagingExceptionHandler = messagingExceptionHandler;
    delegate.setMessagingExceptionHandler(messagingExceptionHandler);
  }
}
