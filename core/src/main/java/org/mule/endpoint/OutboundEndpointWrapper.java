/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.processor.AbstractRedeliveryPolicy;

import java.util.List;
import java.util.Map;

/**
 * Creates a wrapper around a message processor that contains an outbound endpoint.
 *
 * Allows to access the properties of the outbound endpoint while the execution of the
 * outbound endpoint can actually execute another message processor.
 */
public abstract class OutboundEndpointWrapper implements OutboundEndpoint
{
    
    protected abstract MessageProcessor getExecutionMessageProcessor(MuleEvent event) throws MuleException;

    protected abstract OutboundEndpoint getOutboundEndpoint();

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return getExecutionMessageProcessor(event).process(event);
    }

    @Override
    public List<String> getResponseProperties()
    {
        return getOutboundEndpoint().getResponseProperties();
    }

    @Override
    public EndpointURI getEndpointURI()
    {
        return getOutboundEndpoint().getEndpointURI();
    }

    @Override
    public String getAddress()
    {
        return getOutboundEndpoint().getAddress();
    }

    @Override
    public String getEncoding()
    {
        return getOutboundEndpoint().getEncoding();
    }

    @Override
    public Connector getConnector()
    {
        return getOutboundEndpoint().getConnector();
    }

    @Override
    @Deprecated
    public List<Transformer> getTransformers()
    {
        return getOutboundEndpoint().getTransformers();
    }

    @Override
    @Deprecated
    public List<Transformer> getResponseTransformers()
    {
        return getOutboundEndpoint().getResponseTransformers();
    }

    @Override
    public Map getProperties()
    {
        return getOutboundEndpoint().getProperties();
    }

    @Override
    public Object getProperty(Object key)
    {
        return getOutboundEndpoint().getProperty(key);
    }

    @Override
    public String getProtocol()
    {
        return getOutboundEndpoint().getProtocol();
    }

    @Override
    public boolean isReadOnly()
    {
        return getOutboundEndpoint().isReadOnly();
    }

    @Override
    public TransactionConfig getTransactionConfig()
    {
        return getOutboundEndpoint().getTransactionConfig();
    }

    @Override
    public Filter getFilter()
    {
        return getOutboundEndpoint().getFilter();
    }

    @Override
    public boolean isDeleteUnacceptedMessages()
    {
        return getOutboundEndpoint().isDeleteUnacceptedMessages();
    }

    @Override
    @Deprecated
    public EndpointSecurityFilter getSecurityFilter()
    {
        return getOutboundEndpoint().getSecurityFilter();
    }

    @Override
    public EndpointMessageProcessorChainFactory getMessageProcessorsFactory()
    {
        return getOutboundEndpoint().getMessageProcessorsFactory();
    }

    @Override
    public List<MessageProcessor> getMessageProcessors()
    {
        return getOutboundEndpoint().getMessageProcessors();
    }

    @Override
    public List<MessageProcessor> getResponseMessageProcessors()
    {
        return getOutboundEndpoint().getResponseMessageProcessors();
    }

    @Override
    public MessageExchangePattern getExchangePattern()
    {
        return getOutboundEndpoint().getExchangePattern();
    }

    @Override
    public int getResponseTimeout()
    {
        return getOutboundEndpoint().getResponseTimeout();
    }

    @Override
    public String getInitialState()
    {
        return getOutboundEndpoint().getInitialState();
    }

    @Override
    public MuleContext getMuleContext()
    {
        return getOutboundEndpoint().getMuleContext();
    }

    @Override
    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return getOutboundEndpoint().getRetryPolicyTemplate();
    }

    @Override
    public String getEndpointBuilderName()
    {
        return getOutboundEndpoint().getEndpointBuilderName();
    }

    @Override
    public boolean isProtocolSupported(String protocol)
    {
        return getOutboundEndpoint().isProtocolSupported(protocol);
    }

    @Override
    public String getMimeType()
    {
        return getOutboundEndpoint().getMimeType();
    }

    @Override
    public AbstractRedeliveryPolicy getRedeliveryPolicy()
    {
        return getOutboundEndpoint().getRedeliveryPolicy();
    }

    @Override
    public boolean isDisableTransportTransformer()
    {
        return getOutboundEndpoint().isDisableTransportTransformer();
    }

    @Override
    public String getName()
    {
        return getOutboundEndpoint().getName();
    }

    @Override
    public boolean isDynamic()
    {
        return getOutboundEndpoint().isDynamic();
    }
    
}
