/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm;

import static java.util.Collections.emptyMap;

import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.NoReceiverForEndpointException;
import org.mule.compatibility.core.message.CompatibilityMessage;
import org.mule.compatibility.core.message.MuleCompatibilityMessageBuilder;
import org.mule.compatibility.core.transport.AbstractMessageDispatcher;
import org.mule.compatibility.transport.vm.i18n.VMMessages;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.execution.ExecutionCallback;
import org.mule.runtime.core.api.execution.ExecutionTemplate;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.execution.TransactionalExecutionTemplate;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.queue.Queue;
import org.mule.runtime.core.util.queue.QueueSession;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;

/**
 * <code>VMMessageDispatcher</code> is used for providing in memory interaction between components.
 */
public class VMMessageDispatcher extends AbstractMessageDispatcher {

  private final VMConnector connector;

  public VMMessageDispatcher(OutboundEndpoint endpoint) {
    super(endpoint);
    this.connector = (VMConnector) endpoint.getConnector();
  }

  @Override
  protected void doDispatch(final Event event) throws Exception {
    EndpointURI endpointUri = endpoint.getEndpointURI();

    if (endpointUri == null) {
      throw new DispatchException(CoreMessages.objectIsNull("Endpoint"), getEndpoint());
    }
    Event eventToDispatch =
        Event.builder(event).session(new DefaultMuleSession(event.getSession())).variables(emptyMap()).build();
    final MuleCompatibilityMessageBuilder builder =
        new MuleCompatibilityMessageBuilder(createInboundMessage(eventToDispatch.getMessage()));
    builder.correlationId(eventToDispatch.getCorrelationId());
    builder.correlationSequence(eventToDispatch.getGroupCorrelation().getSequence().orElse(null));
    builder.correlationGroupSize(eventToDispatch.getGroupCorrelation().getGroupSize().orElse(null));
    final CompatibilityMessage message = builder.build();

    QueueSession session = getQueueSession();
    Queue queue = session.getQueue(endpointUri.getAddress());
    if (!queue.offer(message, connector.getQueueTimeout())) {
      // queue is full
      throw new DispatchException(VMMessages.queueIsFull(queue.getName(), queue.size()), getEndpoint());
    }
    if (logger.isDebugEnabled()) {
      logger.debug("dispatched MuleEvent on endpointUri: " + endpointUri);
    }
  }

  private QueueSession getQueueSession() throws MuleException {
    return connector.getTransactionalResource(endpoint);
  }

  @Override
  protected InternalMessage doSend(final Event event) throws Exception {
    InternalMessage retMessage;
    final VMMessageReceiver receiver = connector.getReceiver(endpoint.getEndpointURI());
    // Apply any outbound transformers on this event before we dispatch

    if (receiver == null) {
      throw new NoReceiverForEndpointException(VMMessages.noReceiverForEndpoint(connector.getName(), endpoint.getEndpointURI()));
    }

    Event eventToSend =
        Event.builder(event).session(new DefaultMuleSession(event.getSession())).build();
    final MuleCompatibilityMessageBuilder builder =
        new MuleCompatibilityMessageBuilder(createInboundMessage(eventToSend.getMessage()));
    builder.correlationId(eventToSend.getCorrelationId());
    builder.correlationSequence(eventToSend.getGroupCorrelation().getSequence().orElse(null));
    builder.correlationGroupSize(eventToSend.getGroupCorrelation().getGroupSize().orElse(null));
    final CompatibilityMessage message = builder.build();

    ExecutionTemplate<InternalMessage> executionTemplate = TransactionalExecutionTemplate
        .createTransactionalExecutionTemplate(endpoint.getMuleContext(), receiver.getEndpoint().getTransactionConfig());
    ExecutionCallback<InternalMessage> processingCallback = () -> receiver.onCall(message);
    retMessage = executionTemplate.execute(processingCallback);

    if (logger.isDebugEnabled()) {
      logger.debug("sent event on endpointUri: " + endpoint.getEndpointURI());
    }
    if (retMessage != null) {
      retMessage = createInboundMessage(retMessage);
    }
    return retMessage;
  }

  @Override
  protected void doDispose() {
    // template method
  }

  @Override
  protected void doConnect() throws Exception {
    if (!endpoint.getExchangePattern().hasResponse()) {
      // use the default queue profile to configure this queue.
      connector.getQueueProfile().configureQueue(endpoint.getMuleContext(), endpoint.getEndpointURI().getAddress(),
                                                 connector.getQueueManager());
    }
  }

  @Override
  protected void doDisconnect() throws Exception {
    // template method
  }

  private InternalMessage createInboundMessage(InternalMessage message) {
    Map<String, Serializable> outboundProperties = new HashMap<>();
    Map<String, DataHandler> outboundAttachments = new HashMap<>();

    message.getOutboundPropertyNames().stream().forEach(key -> outboundProperties.put(key, message.getOutboundProperty(key)));
    message.getOutboundAttachmentNames().stream()
        .forEach(key -> outboundAttachments.put(key, message.getOutboundAttachment(key)));

    return InternalMessage.builder(message).inboundProperties(outboundProperties).inboundAttachments(outboundAttachments)
        .outboundProperties(emptyMap()).outboundAttachments(emptyMap()).build();
  }

}
