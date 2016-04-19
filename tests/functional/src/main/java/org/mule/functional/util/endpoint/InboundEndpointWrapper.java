/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.util.endpoint;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.EndpointSecurityFilter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;

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

    @Override
    public AbstractRedeliveryPolicy createDefaultRedeliveryPolicy(int maxRedelivery)
    {
        return delegate.createDefaultRedeliveryPolicy(maxRedelivery);
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        delegate.setFlowConstruct(flowConstruct);
    }

    @Override
    public EndpointURI getEndpointURI()
    {
        return delegate.getEndpointURI();
    }

    @Override
    public String getAddress()
    {
        return delegate.getAddress();
    }

    @Override
    public String getEncoding()
    {
        return delegate.getEncoding();
    }

    @Override
    public Connector getConnector()
    {
        return delegate.getConnector();
    }

    @Override
    public Map getProperties()
    {
        return delegate.getProperties();
    }

    @Override
    public Object getProperty(Object key)
    {
        return delegate.getProperty(key);
    }

    @Override
    public String getProtocol()
    {
        return delegate.getProtocol();
    }

    @Override
    public boolean isReadOnly()
    {
        return delegate.isReadOnly();
    }

    @Override
    public TransactionConfig getTransactionConfig()
    {
        return delegate.getTransactionConfig();
    }

    @Override
    public Filter getFilter()
    {
        return delegate.getFilter();
    }

    @Override
    public boolean isDeleteUnacceptedMessages()
    {
        return delegate.isDeleteUnacceptedMessages();
    }

    @Override
    public EndpointSecurityFilter getSecurityFilter()
    {
        return delegate.getSecurityFilter();
    }

    @Override
    public EndpointMessageProcessorChainFactory getMessageProcessorsFactory()
    {
        return delegate.getMessageProcessorsFactory();
    }

    @Override
    public List<MessageProcessor> getMessageProcessors()
    {
        return delegate.getMessageProcessors();
    }

    @Override
    public List<MessageProcessor> getResponseMessageProcessors()
    {
        return delegate.getResponseMessageProcessors();
    }

    @Override
    public MessageExchangePattern getExchangePattern()
    {
        return delegate.getExchangePattern();
    }

    @Override
    public int getResponseTimeout()
    {
        return delegate.getResponseTimeout();
    }

    @Override
    public String getInitialState()
    {
        return delegate.getInitialState();
    }

    @Override
    public MuleContext getMuleContext()
    {
        return delegate.getMuleContext();
    }

    @Override
    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return delegate.getRetryPolicyTemplate();
    }

    @Override
    public String getEndpointBuilderName()
    {
        return delegate.getEndpointBuilderName();
    }

    @Override
    public boolean isProtocolSupported(String protocol)
    {
        return delegate.isProtocolSupported(protocol);
    }

    @Override
    public String getMimeType()
    {
        return delegate.getMimeType();
    }

    @Override
    public AbstractRedeliveryPolicy getRedeliveryPolicy()
    {
        return delegate.getRedeliveryPolicy();
    }

    @Override
    public boolean isDisableTransportTransformer()
    {
        return delegate.isDisableTransportTransformer();
    }

    @Override
    public MuleMessage request(long timeout) throws Exception
    {
        return delegate.request(timeout);
    }

    @Override
    public void setListener(MessageProcessor listener)
    {
        delegate.setListener(listener);
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }

    @Override
    public void start() throws MuleException
    {
        delegate.start();
    }

    @Override
    public void stop() throws MuleException
    {
        delegate.stop();
    }

    public InboundEndpoint getDelegate()
    {
        return delegate;
    }
}
