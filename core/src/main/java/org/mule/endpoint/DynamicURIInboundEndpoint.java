/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.AbstractRedeliveryPolicy;
import org.mule.transport.ConnectException;

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

    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return endpoint.getRetryPolicyTemplate();
    }

    @Override
    public AbstractRedeliveryPolicy getRedeliveryPolicy()
    {
        return endpoint.getRedeliveryPolicy();
    }

    public Connector getConnector()
    {
        return endpoint.getConnector();
    }

    public String getEncoding()
    {
        return endpoint.getEncoding();
    }

    public String getMimeType()
    {
        return endpoint.getMimeType();
    }

    public Filter getFilter()
    {
        return endpoint.getFilter();
    }

    public String getInitialState()
    {
        return endpoint.getInitialState();
    }

    public MuleContext getMuleContext()
    {
        return endpoint.getMuleContext();
    }

    public String getName()
    {
        return endpoint.getName();
    }

    public Map getProperties()
    {
        return endpoint.getProperties();
    }

    public Object getProperty(Object key)
    {
        return endpoint.getProperty(key);
    }

    public String getProtocol()
    {
        return endpoint.getProtocol();
    }

    public int getResponseTimeout()
    {
        return endpoint.getResponseTimeout();
    }

    public EndpointMessageProcessorChainFactory getMessageProcessorsFactory()
    {
        return endpoint.getMessageProcessorsFactory();
    }
    
    public List <MessageProcessor> getMessageProcessors()
    {
        return endpoint.getMessageProcessors();
    }

    public List<MessageProcessor> getResponseMessageProcessors()
    {
        return endpoint.getResponseMessageProcessors();
    }

    public EndpointSecurityFilter getSecurityFilter()
    {
        return endpoint.getSecurityFilter();
    }

    public TransactionConfig getTransactionConfig()
    {
        return endpoint.getTransactionConfig();
    }

    public boolean isDeleteUnacceptedMessages()
    {
        return endpoint.isDeleteUnacceptedMessages();
    }

    public boolean isReadOnly()
    {
        return endpoint.isReadOnly();
    }
    
    public MessageExchangePattern getExchangePattern()
    {
        return endpoint.getExchangePattern();
    }

    public MuleMessage request(long timeout) throws Exception
    {
        return getConnector().request(this, timeout);
    }

    public String getEndpointBuilderName()
    {
        return endpoint.getEndpointBuilderName();
    }

    public boolean isProtocolSupported(String protocol)
    {
        return getConnector().supportsProtocol(protocol);
    }

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

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }
    
}
