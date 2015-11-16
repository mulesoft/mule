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
import org.mule.api.exception.MessagingExceptionHandler;
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
 * Allows EndpointURI to be set and changed dynamically by wrapping up an immutable
 * endpoint instance.
 */
public class DynamicURIOutboundEndpoint implements OutboundEndpoint
{

    private static final long serialVersionUID = -2814979100270307813L;

    protected OutboundEndpoint endpoint;
    private EndpointURI dynamicEndpointURI;

    public DynamicURIOutboundEndpoint(OutboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public DynamicURIOutboundEndpoint(OutboundEndpoint endpoint, EndpointURI dynamicEndpointURI)
    {
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

    public List<String> getResponseProperties()
    {
        return endpoint.getResponseProperties();
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    public String getEndpointBuilderName()
    {
        return endpoint.getEndpointBuilderName();
    }

    public boolean isDisableTransportTransformer()
    {
        return endpoint.isDisableTransportTransformer();
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
        final DynamicURIOutboundEndpoint other = (DynamicURIOutboundEndpoint) obj;
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

    public boolean isProtocolSupported(String protocol)
    {
        return getConnector().supportsProtocol(protocol);
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return endpoint.process(event);
    }

    @Override
    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
        endpoint.setMessagingExceptionHandler(messagingExceptionHandler);
    }

}
