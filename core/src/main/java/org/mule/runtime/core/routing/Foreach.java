/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.api.LocatedMuleException.INFO_LOCATION_KEY;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.NonBlockingSupported;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorPathElement;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.expression.ExpressionConfig;
import org.mule.runtime.core.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.routing.outbound.AbstractMessageSequenceSplitter;
import org.mule.runtime.core.routing.outbound.CollectionMessageSequence;
import org.mule.runtime.core.util.NotificationUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * The {@code foreach} {@link MessageProcessor} allows iterating over a collection payload, or any collection obtained by an
 * expression, generating a message for each element.
 * <p>
 * The number of the message being processed is stored in {@code #[variable:counter]} and the root message is store in
 * {@code #[flowVars.rootMessage]}. Both variables may be renamed by means of {@link #setCounterVariableName(String)} and
 * {@link #setRootMessageVariableName(String)}.
 * <p>
 * Defining a groupSize greater than one, allows iterating over collections of elements of the specified size.
 * <p>
 * The {@link MuleEvent} sent to the next message processor is the same that arrived to foreach.
 */
public class Foreach extends AbstractMessageProcessorOwner implements Initialisable, MessageProcessor, NonBlockingSupported {

  public static final String ROOT_MESSAGE_PROPERTY = "rootMessage";
  public static final String COUNTER_PROPERTY = "counter";

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private List<MessageProcessor> messageProcessors;
  private MessageProcessor ownedMessageProcessor;
  private AbstractMessageSequenceSplitter splitter;
  private MessageFilter filter;
  private String collectionExpression;
  private ExpressionConfig expressionConfig = new ExpressionConfig();
  private int batchSize;
  private String rootMessageVariableName;
  private String counterVariableName;
  private boolean xpathCollection;
  private volatile boolean messageProcessorInitialized;

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    String parentMessageProp = rootMessageVariableName != null ? rootMessageVariableName : ROOT_MESSAGE_PROPERTY;
    Object previousCounterVar = null;
    Object previousRootMessageVar = null;
    if (event.getFlowVariableNames().contains(counterVariableName)) {
      previousCounterVar = event.getFlowVariable(counterVariableName);
    }
    if (event.getFlowVariableNames().contains(parentMessageProp)) {
      previousRootMessageVar = event.getFlowVariable(parentMessageProp);
    }
    MuleMessage message = event.getMessage();
    boolean transformed = false;
    if (xpathCollection) {
      MuleMessage transformedMessage = transformPayloadIfNeeded(message);
      if (transformedMessage != message) {
        transformed = true;
        message = transformedMessage;
        event.setMessage(transformedMessage);
      }
    }
    event.setFlowVariable(parentMessageProp, message);
    doProcess(event);
    if (transformed) {
      event.setMessage(transformBack(message));
    }
    if (previousCounterVar != null) {
      event.setFlowVariable(counterVariableName, previousCounterVar);
    } else {
      event.removeFlowVariable(counterVariableName);
    }
    if (previousRootMessageVar != null) {
      event.setFlowVariable(parentMessageProp, previousRootMessageVar);
    } else {
      event.removeFlowVariable(parentMessageProp);
    }
    return event;
  }

  protected void doProcess(MuleEvent event) throws MuleException, MessagingException {
    try {
      ownedMessageProcessor.process(event);
    } catch (MessagingException e) {
      if (splitter.equals(e.getFailingMessageProcessor()) || filter.equals(e.getFailingMessageProcessor())) {
        // Make sure the context information for the exception is relative to the ForEach.
        e.getInfo().remove(INFO_LOCATION_KEY);
        throw new MessagingException(event, e, this);
      } else {
        throw e;
      }
    }
  }

  private MuleMessage transformPayloadIfNeeded(MuleMessage message) throws TransformerException {
    Object payload = message.getPayload();
    if (payload instanceof Document || payload.getClass().getName().startsWith("org.dom4j.")) {
      return message;
    } else {
      return muleContext.getTransformationService().transform(message, DataType.fromType(Document.class));
    }
  }

  private MuleMessage transformBack(MuleMessage message) throws TransformerException {
    return muleContext.getTransformationService().transform(message, DataType.STRING);
  }

  @Override
  protected List<MessageProcessor> getOwnedMessageProcessors() {
    return messageProcessors;
  }

  @Override
  public void addMessageProcessorPathElements(MessageProcessorPathElement pathElement) {
    List<MessageProcessor> processors;
    if (messageProcessorInitialized) {
      // Skips the splitter that is added at the beginning and the filter at the end
      processors = getOwnedMessageProcessors().subList(1, getOwnedMessageProcessors().size() - 1);
    } else {
      processors = getOwnedMessageProcessors();
    }
    NotificationUtils.addMessageProcessorPathElements(processors, pathElement);
  }

  public void setMessageProcessors(List<MessageProcessor> messageProcessors) throws MuleException {
    this.messageProcessors = messageProcessors;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (collectionExpression != null) {
      expressionConfig.setExpression(collectionExpression);
      splitter = new ExpressionSplitter(expressionConfig);
      if (isXPathExpression(expressionConfig.getExpression())) {
        xpathCollection = true;
      }
    } else {
      splitter = new CollectionMapSplitter();
    }
    splitter.setBatchSize(batchSize);
    splitter.setCounterVariableName(counterVariableName);
    splitter.setMuleContext(muleContext);
    messageProcessors.add(0, splitter);
    filter = new MessageFilter(message -> false);
    messageProcessors.add(filter);
    messageProcessorInitialized = true;

    try {
      this.ownedMessageProcessor = new DefaultMessageProcessorChainBuilder(muleContext).chain(messageProcessors).build();
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
    super.initialise();
  }

  private boolean isXPathExpression(String expression) {
    return expression.matches("^xpath\\(.+\\)$") || expression.matches("^xpath3\\(.+\\)$");
  }

  public void setCollectionExpression(String expression) {
    this.collectionExpression = expression;
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

  private static class CollectionMapSplitter extends CollectionSplitter {

    @Override
    protected MessageSequence<?> splitMessageIntoSequence(MuleEvent event) {
      Object payload = event.getMessage().getPayload();
      if (payload instanceof Map<?, ?>) {
        List<MuleEvent> list = new LinkedList<>();
        Set<Map.Entry<?, ?>> set = ((Map) payload).entrySet();
        for (Entry<?, ?> entry : set) {
          // TODO MULE-9502 Support "key" flowVar with MapSplitter in Mule 4
          list.add(MuleEvent.builder(event).message(MuleMessage.builder().payload(entry.getValue()).build()).build());
        }
        return new CollectionMessageSequence(list);
      }
      return super.splitMessageIntoSequence(event);
    }

  }
}
