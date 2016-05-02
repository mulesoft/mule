/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.execution.FlowProcessingPhaseTemplate;
import org.mule.runtime.core.execution.MessageProcessContext;
import org.mule.runtime.core.execution.ValidationPhaseTemplate;
import org.mule.runtime.core.util.ObjectUtils;

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

    @Override
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

    @Override
    public void afterFailureProcessingFlow(MessagingException messagingException) throws MuleException
    {
    }

    @Override
    public void afterFailureProcessingFlow(MuleException exception) throws MuleException
    {
    }

    @Override
    public MuleEvent routeEvent(MuleEvent muleEvent) throws MuleException
    {
        MuleEvent response = messageReceiver.routeEvent(muleEvent);
        if (!messageReceiver.getEndpoint().getExchangePattern().hasResponse())
        {
            return null;
        }
        return response;
    }

    @Override
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

