/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint;

import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.EndpointSecurityFilter;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;

import java.util.List;
import java.util.Map;

/**
 * Allow's EndpointURI to be set and changed dynamically by wrapping up an immutable endpoint instance.
 */
public class DynamicURIInboundEndpoint implements InboundEndpoint
{

    private static final long serialVersionUID = -2814979100270307813L;

    protected InboundEndpoint endpoint;
    private EndpointURI dynamicEndpointURI;
    private MessageProcessor listener;
    private FlowConstruct flowConstruct;

    public DynamicURIInboundEndpoint(InboundEndpoint endpoint)
    {
        this(endpoint, null);
    }

    public DynamicURIInboundEndpoint(InboundEndpoint endpoint, EndpointURI dynamicEndpointURI)
    {
//        if (endpoint instanceof DynamicURIInboundEndpoint) 
//        {
//            throw new IllegalArgumentException("Dynamic endpoints can only wrap immuntable InboundEndpoint instances!");
//        }
//        
        this.endpoint = endpoint;
        setEndpointURI(dynamicEndpointURI);
    }

    @Override
    public EndpointURI getEndpointURI()
    {
        if (dynamicEndpointURI != null)
        {
            return dynamicEndpointURI;
        }
        else
        {
            return endpoint.getEndpointURI();
        }
    }

    @Override
    public String getAddress()
    {
        EndpointURI uri = getEndpointURI();
        if (uri != null)
        {
            return uri.getUri().toString();
        }
        else
        {
            return null;
        }
    }

    public void setEndpointURI(EndpointURI dynamicEndpointURI)
    {
        this.dynamicEndpointURI = dynamicEndpointURI;
    }

    @Override
    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return endpoint.getRetryPolicyTemplate();
    }

    @Override
    public AbstractRedeliveryPolicy getRedeliveryPolicy()
    {
        return endpoint.getRedeliveryPolicy();
    }

    @Override
    public Connector getConnector()
    {
        return endpoint.getConnector();
    }

    @Override
    public String getEncoding()
    {
        return endpoint.getEncoding();
    }

    @Override
    public String getMimeType()
    {
        return endpoint.getMimeType();
    }

    @Override
    public Filter getFilter()
    {
        return endpoint.getFilter();
    }

    @Override
    public String getInitialState()
    {
        return endpoint.getInitialState();
    }

    @Override
    public MuleContext getMuleContext()
    {
        return endpoint.getMuleContext();
    }

    @Override
    public String getName()
    {
        return endpoint.getName();
    }

    @Override
    public Map getProperties()
    {
        return endpoint.getProperties();
    }

    @Override
    public Object getProperty(Object key)
    {
        return endpoint.getProperty(key);
    }

    @Override
    public String getProtocol()
    {
        return endpoint.getProtocol();
    }

    @Override
    public int getResponseTimeout()
    {
        return endpoint.getResponseTimeout();
    }

    @Override
    public EndpointMessageProcessorChainFactory getMessageProcessorsFactory()
    {
        return endpoint.getMessageProcessorsFactory();
    }
    
    @Override
    public List <MessageProcessor> getMessageProcessors()
    {
        return endpoint.getMessageProcessors();
    }

    @Override
    public List<MessageProcessor> getResponseMessageProcessors()
    {
        return endpoint.getResponseMessageProcessors();
    }

    @Override
    public EndpointSecurityFilter getSecurityFilter()
    {
        return endpoint.getSecurityFilter();
    }

    @Override
    public TransactionConfig getTransactionConfig()
    {
        return endpoint.getTransactionConfig();
    }

    @Override
    public boolean isDeleteUnacceptedMessages()
    {
        return endpoint.isDeleteUnacceptedMessages();
    }

    @Override
    public boolean isReadOnly()
    {
        return endpoint.isReadOnly();
    }
    
    @Override
    public MessageExchangePattern getExchangePattern()
    {
        return endpoint.getExchangePattern();
    }

    @Override
    public MuleMessage request(long timeout) throws Exception
    {
        return getConnector().request(this, timeout);
    }

    @Override
    public String getEndpointBuilderName()
    {
        return endpoint.getEndpointBuilderName();
    }

    @Override
    public boolean isProtocolSupported(String protocol)
    {
        return getConnector().supportsProtocol(protocol);
    }

    @Override
    public boolean isDisableTransportTransformer()
    {
        return endpoint.isDisableTransportTransformer();
    }

    @Override
    public AbstractRedeliveryPolicy createDefaultRedeliveryPolicy(int maxRedelivery)
    {
        return endpoint.createDefaultRedeliveryPolicy(maxRedelivery);
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dynamicEndpointURI == null) ? 0 : dynamicEndpointURI.hashCode());
        result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final DynamicURIInboundEndpoint other = (DynamicURIInboundEndpoint) obj;
        if (dynamicEndpointURI == null)
        {
            if (other.dynamicEndpointURI != null)
            {
                return false;
            }
        }
        else if (!dynamicEndpointURI.equals(other.dynamicEndpointURI))
        {
            return false;
        }
        if (endpoint == null)
        {
            if (other.endpoint != null)
            {
                return false;
            }
        }
        else if (!endpoint.equals(other.endpoint))
        {
            return false;
        }
        return true;
    }

    @Override
    public void start() throws MuleException
    {
        try
        {
            getConnector().registerListener(this, listener, flowConstruct);
        }
        // Let connection exceptions bubble up to trigger the reconnection strategy.
        catch (ConnectException ce)
        {
            throw ce;
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStartInboundEndpoint(this), e, this);
        }
    }

    @Override
    public void stop() throws MuleException
    {
        try
        {
            getConnector().unregisterListener(this, flowConstruct);
        }
        catch (Exception e)
        {
            throw new LifecycleException(CoreMessages.failedToStartInboundEndpoint(this), e, this);
        }
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    @Override
    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }
    
}
