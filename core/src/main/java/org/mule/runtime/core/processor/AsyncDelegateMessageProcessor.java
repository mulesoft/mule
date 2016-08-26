/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import static java.util.Collections.emptyMap;
import static org.mule.runtime.core.DefaultMuleEvent.setCurrentEvent;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorContainer;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.api.processor.StageNameSource;
import org.mule.runtime.core.api.processor.StageNameSourceProvider;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.util.NotificationUtils;
import org.mule.runtime.core.work.MuleWorkManager;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes {@link MuleEvent}'s asynchronously using a {@link MuleWorkManager} to schedule asynchronous processing of
 * MessageProcessor delegate configured the next {@link MessageProcessor}. The next {@link MessageProcessor} is therefore be
 * executed in a different thread regardless of the exchange-pattern configured on the inbound endpoint. If a transaction is
 * present then an exception is thrown.
 */
public class AsyncDelegateMessageProcessor extends AbstractMessageProcessorOwner
    implements MessageProcessor, Initialisable, Startable, Stoppable, NonBlockingSupported {

  protected Logger logger = LoggerFactory.getLogger(getClass());
  private AtomicBoolean consumablePayloadWarned = new AtomicBoolean(false);

  protected MessageProcessor delegate;

  protected List<MessageProcessor> processors;
  protected ProcessingStrategy processingStrategy;
  protected String name;

  private MessageProcessor target;

  public AsyncDelegateMessageProcessor(MessageProcessor delegate,
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

    MessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(flowConstruct);
    processingStrategy.configureProcessors(Collections.singletonList(delegate), nameSource, builder,
                                           muleContext);
    try {
      target = builder.build();
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
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
  public MuleEvent process(MuleEvent event) throws MuleException {
    if (event.isTransacted()) {
      throw new MessagingException(CoreMessages.asyncDoesNotSupportTransactions(), event, this);
    }

    final MuleMessage message = event.getMessage();
    if (consumablePayloadWarned.compareAndSet(false, true) && message.getDataType().isStreamType()) {
      logger.warn(String.format("Using 'async' router with consumable payload (%s) may lead to unexpected results." +
          " Please ensure that only one of the branches actually consumes the payload, or transform it by using an <object-to-byte-array-transformer>.",
                                message.getPayload().getClass().getName()));
    }

    if (target != null) {
      // Clone event, make it async and remove ReplyToHandler
      MuleEvent newEvent = MuleEvent.builder(event).message(message).flowVariables(emptyMap()).synchronous(false)
          .exchangePattern(ONE_WAY).replyToHandler(null).build();
      // Update RequestContext ThreadLocal for backwards compatibility
      setCurrentEvent(newEvent);
      target.process(newEvent);
    }
    return VoidMuleEvent.getInstance();
  }

  public void setDelegate(MessageProcessor delegate) {
    this.delegate = delegate;
  }

  @Override
  protected List<MessageProcessor> getOwnedMessageProcessors() {
    return Collections.singletonList(target);
  }

  public ProcessingStrategy getProcessingStrategy() {
    return processingStrategy;
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    if (delegate instanceof MessageProcessorContainer) {
      ((MessageProcessorContainer) delegate).addMessageProcessorPathElements(pathElement);
    } else {
      NotificationUtils.addMessageProcessorPathElements(Collections.singletonList(delegate), pathElement);
    }
  }

}
