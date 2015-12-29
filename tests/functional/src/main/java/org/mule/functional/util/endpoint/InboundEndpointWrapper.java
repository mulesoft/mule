/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.util.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.Connector;
import org.mule.processor.AbstractRedeliveryPolicy;

import java.util.List;
import java.util.Map;

/**
 * Wraps an {@link InboundEndpoint} enabling subclasses to override only those
 * methods which add extra behaviour.
 */
public abstract class InboundEndpointWrapper implements InboundEndpoint
{

    private InboundEndpoint delegate;

    public InboundEndpointWrapper(InboundEndpoint delegate)
    {
        this.delegate = delegate;
    }

    public AbstractRedeliveryPolicy createDefaultRedeliveryPolicy(int maxRedelivery)
    {
        return delegate.createDefaultRedeliveryPolicy(maxRedelivery);
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        delegate.setFlowConstruct(flowConstruct);
    }

    public EndpointURI getEndpointURI()
    {
        return delegate.getEndpointURI();
    }

    public String getAddress()
    {
        return delegate.getAddress();
    }

    public String getEncoding()
    {
        return delegate.getEncoding();
    }

    public Connector getConnector()
    {
        return delegate.getConnector();
    }

    public Map getProperties()
    {
        return delegate.getProperties();
    }

    public Object getProperty(Object key)
    {
        return delegate.getProperty(key);
    }

    public String getProtocol()
    {
        return delegate.getProtocol();
    }

    public boolean isReadOnly()
    {
        return delegate.isReadOnly();
    }

    public TransactionConfig getTransactionConfig()
    {
        return delegate.getTransactionConfig();
    }

    public Filter getFilter()
    {
        return delegate.getFilter();
    }

    public boolean isDeleteUnacceptedMessages()
    {
        return delegate.isDeleteUnacceptedMessages();
    }

    public EndpointSecurityFilter getSecurityFilter()
    {
        return delegate.getSecurityFilter();
    }

    public EndpointMessageProcessorChainFactory getMessageProcessorsFactory()
    {
        return delegate.getMessageProcessorsFactory();
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return delegate.getMessageProcessors();
    }

    public List<MessageProcessor> getResponseMessageProcessors()
    {
        return delegate.getResponseMessageProcessors();
    }

    public MessageExchangePattern getExchangePattern()
    {
        return delegate.getExchangePattern();
    }

    public int getResponseTimeout()
    {
        return delegate.getResponseTimeout();
    }

    public String getInitialState()
    {
        return delegate.getInitialState();
    }

    public MuleContext getMuleContext()
    {
        return delegate.getMuleContext();
    }

    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return delegate.getRetryPolicyTemplate();
    }

    public String getEndpointBuilderName()
    {
        return delegate.getEndpointBuilderName();
    }

    public boolean isProtocolSupported(String protocol)
    {
        return delegate.isProtocolSupported(protocol);
    }

    public String getMimeType()
    {
        return delegate.getMimeType();
    }

    public AbstractRedeliveryPolicy getRedeliveryPolicy()
    {
        return delegate.getRedeliveryPolicy();
    }

    public boolean isDisableTransportTransformer()
    {
        return delegate.isDisableTransportTransformer();
    }

    public MuleMessage request(long timeout) throws Exception
    {
        return delegate.request(timeout);
    }

    public void setListener(MessageProcessor listener)
    {
        delegate.setListener(listener);
    }

    public String getName()
    {
        return delegate.getName();
    }

    public void start() throws MuleException
    {
        delegate.start();
    }

    public void stop() throws MuleException
    {
        delegate.stop();
    }

    public InboundEndpoint getDelegate()
    {
        return delegate;
    }
}
