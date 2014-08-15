/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.source.MessageSource;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.PropertyScope;
import org.mule.execution.FlowProcessingPhaseTemplate;
import org.mule.execution.MessageProcessContext;
import org.mule.execution.ValidationPhaseTemplate;
import org.mule.util.ObjectUtils;

import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractTransportMessageProcessTemplate<MessageReceiverType extends AbstractMessageReceiver, ConnectorType extends AbstractConnector> implements FlowProcessingPhaseTemplate, ValidationPhaseTemplate
{

    protected transient Log logger = LogFactory.getLog(getClass());

    private final MessageReceiverType messageReceiver;
    private Object rawMessage;

    public AbstractTransportMessageProcessTemplate(MessageReceiverType messageReceiver)
    {
        this.messageReceiver = messageReceiver;
    }

    public MuleEvent getMuleEvent() throws MuleException
    {
        MuleMessage messageFromSource = createMessageFromSource(getOriginalMessage());
        return createEventFromMuleMessage(messageFromSource);
    }

    @Override
    public Object getOriginalMessage() throws MuleException
    {

        if (this.rawMessage == null)
        {
            this.rawMessage = acquireMessage();
        }
        return this.rawMessage;
    }

    public void afterFailureProcessingFlow(MessagingException messagingException) throws MuleException
    {
    }

    @Override
    public void afterFailureProcessingFlow(MuleException exception) throws MuleException
    {
    }

    public MuleEvent routeEvent(MuleEvent muleEvent) throws MuleException
    {
        return messageReceiver.routeEvent(muleEvent);
    }

    public void afterSuccessfulProcessingFlow(MuleEvent response) throws MuleException
    {
    }

    /**
     * This method will only be called once for the {@link MessageProcessContext}
     *
     * @return the raw message from the {@link MessageSource}
     * @throws MuleException
     */
    public abstract Object acquireMessage() throws MuleException;

    protected void propagateRootMessageIdProperty(MuleMessage message)
    {
        String rootId = message.getInboundProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY);
        if (rootId != null)
        {
            message.setMessageRootId(rootId);
            message.removeProperty(MuleProperties.MULE_ROOT_MESSAGE_ID_PROPERTY, PropertyScope.INBOUND);
        }
    }

    @Override
    public boolean validateMessage()
    {
        return true;
    }

    @Override
    public void discardInvalidMessage() throws MuleException
    {
    }

    protected void warnIfMuleClientSendUsed(MuleMessage message)
    {
        final Object remoteSyncProperty = message.removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY,
                                                                 PropertyScope.INBOUND);
        if (ObjectUtils.getBoolean(remoteSyncProperty, false) && !messageReceiver.getEndpoint().getExchangePattern().hasResponse())
        {
            logger.warn("MuleClient.send() was used but inbound endpoint "
                        + messageReceiver.getEndpoint().getEndpointURI().getUri().toString()
                        + " is not 'request-response'.  No response will be returned.");
        }

        message.removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY, PropertyScope.INBOUND);
    }

    protected MuleEvent createEventFromMuleMessage(MuleMessage muleMessage) throws MuleException
    {
        MuleEvent muleEvent = messageReceiver.createMuleEvent(muleMessage, getOutputStream());
        if (!messageReceiver.getEndpoint().isDisableTransportTransformer())
        {
            messageReceiver.applyInboundTransformers(muleEvent);
        }
        return muleEvent;
    }
    
    protected OutputStream getOutputStream()
    {
        return null;
    }

    protected MuleMessage createMessageFromSource(Object message) throws MuleException
    {
        MuleMessage muleMessage = messageReceiver.createMuleMessage(message, messageReceiver.getEndpoint().getEncoding());
        warnIfMuleClientSendUsed(muleMessage);
        propagateRootMessageIdProperty(muleMessage);
        return muleMessage;
    }

    protected MessageReceiverType getMessageReceiver()
    {
        return this.messageReceiver;
    }

    protected InboundEndpoint getInboundEndpoint()
    {
        return this.messageReceiver.getEndpoint();
    }

    @SuppressWarnings("unchecked")
    protected ConnectorType getConnector()
    {
        return (ConnectorType) this.messageReceiver.getConnector();
    }

    protected MuleContext getMuleContext()
    {
        return this.messageReceiver.getEndpoint().getMuleContext();
    }

    public FlowConstruct getFlowConstruct()
    {
        return this.messageReceiver.getFlowConstruct();
    }

    @Override
    public MuleEvent beforeRouteEvent(MuleEvent muleEvent) throws MuleException
    {
        return muleEvent;
    }

    @Override
    public MuleEvent afterRouteEvent(MuleEvent muleEvent) throws MuleException
    {
        return muleEvent;
    }

}

