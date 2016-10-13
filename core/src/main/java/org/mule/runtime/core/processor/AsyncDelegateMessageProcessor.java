/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.api.Event.setCurrentEvent;
import static org.mule.runtime.core.config.i18n.CoreMessages.asyncDoesNotSupportTransactions;
import static org.mule.runtime.core.util.rx.Exceptions.checkedConsumer;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.StageNameSource;
import org.mule.runtime.core.api.processor.StageNameSourceProvider;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.core.work.MuleWorkManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes {@link Event}'s asynchronously using a {@link MuleWorkManager} to schedule asynchronous processing of
 * MessageProcessor delegate configured the next {@link Processor}. The next {@link Processor} is therefore be executed in a
 * different thread regardless of the exchange-pattern configured on the inbound endpoint. If a transaction is present then an
 * exception is thrown.
 */
public class AsyncDelegateMessageProcessor extends AbstractMessageProcessorOwner
    implements Processor, Initialisable, Startable, Stoppable {

  protected Logger logger = LoggerFactory.getLogger(getClass());
  private AtomicBoolean consumablePayloadWarned = new AtomicBoolean(false);

  protected Processor delegate;

  protected List<Processor> processors;
  protected ProcessingStrategy processingStrategy;
  protected String name;

  private Processor target;

  public AsyncDelegateMessageProcessor(Processor delegate,
                                       ProcessingStrategy processingStrategy,
                                       String name) {
    this.delegate = delegate;
    this.processingStrategy = processingStrategy;
    this.name = name;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (delegate == null) {
      throw new InitialisationException(CoreMessages.objectIsNull("delegate message processor"), this);
    }
    if (processingStrategy == null) {
      throw new InitialisationException(CoreMessages.objectIsNull("processingStrategy"), this);
    }

    validateFlowConstruct();

    StageNameSource nameSource = null;

    if (name != null) {
      nameSource = ((StageNameSourceProvider) flowConstruct).getAsyncStageNameSource(name);
    } else {
      nameSource = ((StageNameSourceProvider) flowConstruct).getAsyncStageNameSource();
    }

    MessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    processingStrategy.configureProcessors(Collections.singletonList(delegate), nameSource, builder,
                                           muleContext);
    target = builder.build();
    super.initialise();
  }

  private void validateFlowConstruct() {
    if (flowConstruct == null) {
      throw new IllegalArgumentException("FlowConstruct cannot be null");
    } else if (!(flowConstruct instanceof StageNameSourceProvider)) {
      throw new IllegalArgumentException(String
          .format("FlowConstuct must implement the %s interface. However, the type %s does not implement it",
                  StageNameSourceProvider.class.getCanonicalName(), flowConstruct.getClass().getCanonicalName()));
    }
  }

  @Override
  public Event process(Event event) throws MuleException {
    assertNotTransactional(event);

    final InternalMessage message = event.getMessage();
    warnConsumablePayload(message);

    if (target != null) {
      target.process(updateEventForAsync(event));
    }
    return event;
  }

  private void assertNotTransactional(Event event) throws RoutingException {
    if (event.isTransacted()) {
      throw new RoutingException(asyncDoesNotSupportTransactions(), delegate);
    }
  }

  @Override
  public Publisher<Event> apply(Publisher<Event> publisher) {
    return from(publisher).concatMap(request -> just(request)
        .doOnNext(checkedConsumer(event -> assertNotTransactional(event)))
        .doOnNext(event -> warnConsumablePayload(event.getMessage())).map(event -> updateEventForAsync(event)).transform(target)
        .map(event -> request));
  }

  private Event updateEventForAsync(Event event) {
    // Clone event, make it async and remove ReplyToHandler
    Event newEvent = Event.builder(event).synchronous(false).exchangePattern(ONE_WAY).replyToHandler(null).build();
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

  public void setDelegate(Processor delegate) {
    this.delegate = delegate;
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return Collections.singletonList(target);
  }

  public ProcessingStrategy getProcessingStrategy() {
    return processingStrategy;
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    NotificationUtils.addMessageProcessorPathElements(delegate, pathElement.addChild(this));
  }

}
