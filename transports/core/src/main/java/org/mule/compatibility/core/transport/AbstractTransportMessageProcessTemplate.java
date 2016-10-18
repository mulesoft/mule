/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_REMOTE_SYNC_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY;

import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.message.CompatibilityMessage;
import org.mule.compatibility.core.message.MuleCompatibilityMessageBuilder;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.execution.FlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.ValidationPhaseTemplate;
import org.mule.runtime.core.util.ObjectUtils;

import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransportMessageProcessTemplate<MessageReceiverType extends AbstractMessageReceiver, ConnectorType extends AbstractConnector>
    implements FlowProcessingPhaseTemplate, ValidationPhaseTemplate {

  protected transient Logger logger = LoggerFactory.getLogger(getClass());

  private final MessageReceiverType messageReceiver;
  private Object rawMessage;
  private Event muleEvent;

  public AbstractTransportMessageProcessTemplate(MessageReceiverType messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  @Override
  public Event getEvent() throws MuleException {
    if (muleEvent == null) {
      CompatibilityMessage messageFromSource = createMessageFromSource(getOriginalMessage());
      muleEvent = createEventFromMuleMessage(messageFromSource);
    }
    return muleEvent;
  }

  @Override
  public Object getOriginalMessage() throws MuleException {

    if (this.rawMessage == null) {
      this.rawMessage = acquireMessage();
    }
    return this.rawMessage;
  }

  @Override
  public void afterFailureProcessingFlow(MessagingException messagingException) throws MuleException {}

  @Override
  public void afterFailureProcessingFlow(MuleException exception) throws MuleException {}

  @Override
  public Event routeEvent(Event muleEvent) throws MuleException {
    Event response = messageReceiver.routeEvent(muleEvent);
    if (!messageReceiver.getEndpoint().getExchangePattern().hasResponse()) {
      return null;
    }
    return response;
  }

  @Override
  public void afterSuccessfulProcessingFlow(Event response) throws MuleException {}

  /**
   * This method will only be called once for the {@link MessageProcessContext}
   *
   * @return the raw message from the {@link MessageSource}
   * @throws MuleException
   */
  public abstract Object acquireMessage() throws MuleException;

  protected CompatibilityMessage propagateRootMessageIdProperty(CompatibilityMessage message) {
    String rootId = message.getInboundProperty(MULE_ROOT_MESSAGE_ID_PROPERTY);
    if (rootId != null) {
      final MuleCompatibilityMessageBuilder builder = new MuleCompatibilityMessageBuilder(message);
      builder.correlationId(rootId).removeInboundProperty(MULE_ROOT_MESSAGE_ID_PROPERTY);
      return builder.build();
    } else {
      return message;
    }
  }

  @Override
  public boolean validateMessage() {
    return true;
  }

  @Override
  public void discardInvalidMessage() throws MuleException {}

  protected CompatibilityMessage warnIfMuleClientSendUsed(CompatibilityMessage message) {
    MuleCompatibilityMessageBuilder messageBuilder = new MuleCompatibilityMessageBuilder(message);
    final Object remoteSyncProperty = message.getInboundProperty(MULE_REMOTE_SYNC_PROPERTY);
    messageBuilder.removeInboundProperty(MULE_REMOTE_SYNC_PROPERTY);
    if (ObjectUtils.getBoolean(remoteSyncProperty, false) && !messageReceiver.getEndpoint().getExchangePattern().hasResponse()) {
      logger.warn("MuleClient.send() was used but inbound endpoint "
          + messageReceiver.getEndpoint().getEndpointURI().getUri().toString()
          + " is not 'request-response'.  No response will be returned.");
    }
    return messageBuilder.build();
  }

  protected Event createEventFromMuleMessage(CompatibilityMessage muleMessage) throws MuleException {
    Event muleEvent = messageReceiver.createMuleEvent(muleMessage, getOutputStream());
    if (!messageReceiver.getEndpoint().isDisableTransportTransformer()) {
      messageReceiver.applyInboundTransformers(muleEvent);
    }
    return muleEvent;
  }

  protected OutputStream getOutputStream() {
    return null;
  }

  protected CompatibilityMessage createMessageFromSource(Object message) throws MuleException {
    CompatibilityMessage muleMessage =
        messageReceiver.createMuleMessage(message, messageReceiver.getEndpoint().getEncoding());
    muleMessage = warnIfMuleClientSendUsed(muleMessage);
    muleMessage = propagateRootMessageIdProperty(muleMessage);
    return muleMessage;
  }

  protected MessageReceiverType getMessageReceiver() {
    return this.messageReceiver;
  }

  protected InboundEndpoint getInboundEndpoint() {
    return this.messageReceiver.getEndpoint();
  }

  @SuppressWarnings("unchecked")
  protected ConnectorType getConnector() {
    return (ConnectorType) this.messageReceiver.getConnector();
  }

  protected MuleContext getMuleContext() {
    return this.messageReceiver.getEndpoint().getMuleContext();
  }

  public FlowConstruct getFlowConstruct() {
    return this.messageReceiver.getFlowConstruct();
  }

  @Override
  public Event beforeRouteEvent(Event muleEvent) throws MuleException {
    return muleEvent;
  }

  @Override
  public Event afterRouteEvent(Event muleEvent) throws MuleException {
    return muleEvent;
  }

}

