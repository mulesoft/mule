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
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.api.transport.DispatchException;
import org.mule.processor.AbstractRedeliveryPolicy;
import org.mule.transport.AbstractConnector;
import org.mule.util.ObjectNameHelper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An Outbound endpoint who's URI is a template used to created new non dynamic
 * endpoints based on the current message.
 * This allows for the destination of a message to change based on the contents
 * of the message. Note that this endpoint ONLY substitutes the URI, but other
 * config elements such as the transformers, filters, etc do not change. You
 * cannot change an endpoint scheme dynamically so you can't switch between
 * HTTP and JMS for example using the same dynamic endpoint.
 */
public class DynamicOutboundEndpoint implements OutboundEndpoint
{

    protected transient final Log logger = LogFactory.getLog(DynamicOutboundEndpoint.class);

    private static final long serialVersionUID = 8861985949279708638L;

    private final EndpointBuilder endpointBuilder;

    private final OutboundEndpoint prototypeEndpoint;

    // Caches resolved static endpoints to improve performance
    private final Map<String, OutboundEndpoint> staticEndpoints = Collections.synchronizedMap(new LRUMap(64));

    private final DynamicURIBuilder dynamicURIBuilder;
    
    private MessagingExceptionHandler exceptionHandler;

    public DynamicOutboundEndpoint(EndpointBuilder endpointBuilder, DynamicURIBuilder dynamicURIBuilder)
    {
        this.endpointBuilder = endpointBuilder;
        this.dynamicURIBuilder = dynamicURIBuilder;

        try
        {
            prototypeEndpoint = endpointBuilder.buildOutboundEndpoint();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private Properties getServiceOverrides() throws EndpointException
    {
        Properties properties = null;

        if (endpointBuilder instanceof AbstractEndpointBuilder)
        {
            Connector connector = ((AbstractEndpointBuilder) this.endpointBuilder).getConnector();

            if (connector instanceof AbstractConnector && ((AbstractConnector) connector).getServiceOverrides() != null)
            {
                properties = new Properties();
                properties.putAll(((AbstractConnector) connector).getServiceOverrides());
            }
        }

        return properties;
    }

    public OutboundEndpoint getStaticEndpoint(MuleEvent event)  throws MuleException
    {
        final String uri = resolveUri(event);

        OutboundEndpoint outboundEndpoint = staticEndpoints.get(uri);

        if (outboundEndpoint == null)
        {
            final EndpointURI endpointURIForMessage = createEndpointUri(uri);
            outboundEndpoint = createStaticEndpoint(endpointURIForMessage);
            staticEndpoints.put(endpointURIForMessage.getAddress(), outboundEndpoint);
        }

        return outboundEndpoint;
    }

    private EndpointURI createEndpointUri(String uri) throws EndpointException, InitialisationException
    {
        final MuleEndpointURI endpointUri = new MuleEndpointURI(uri, getMuleContext(), getServiceOverrides());
        endpointUri.initialise();

        return endpointUri;
    }

    private String resolveUri(MuleEvent event) throws DispatchException
    {
        try
        {
            return dynamicURIBuilder.build(event);
        }
        catch (Exception e)
        {
            throw new DispatchException(event, this, e);
        }
    }

    private OutboundEndpoint createStaticEndpoint(EndpointURI uri) throws EndpointException, InitialisationException
    {
        try
        {
            EndpointBuilder staticBuilder = (EndpointBuilder) endpointBuilder.clone();
            staticBuilder.setURIBuilder(new URIBuilder(uri));
            String endpointName = ObjectNameHelper.getEndpointNameFor(uri);
            staticBuilder.setName(endpointName);
            OutboundEndpoint endpoint = staticBuilder.buildOutboundEndpoint();
            endpoint.setMessagingExceptionHandler(exceptionHandler);
            return endpoint;
        }
        catch (CloneNotSupportedException e)
        {
            // This cannot happen, because we implement Cloneable
            throw new IllegalStateException("Unable to clone endpoint builder");
        }
    }

    @Override
    public boolean equals(Object o)
    {
        return this == o;
    }

    @Override
    public int hashCode()
    {
        return System.identityHashCode(this);
    }

    public Connector getConnector()
    {
        throw new UnsupportedOperationException("No connector available");
    }

    public EndpointURI getEndpointURI()
    {
        return null;
    }

    @Override
    public AbstractRedeliveryPolicy getRedeliveryPolicy()
    {
        return prototypeEndpoint.getRedeliveryPolicy();
    }

    public String getAddress()
    {
        return dynamicURIBuilder.getUriTemplate();
    }

    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return prototypeEndpoint.getRetryPolicyTemplate();
    }

    public String getEncoding()
    {
        return prototypeEndpoint.getEncoding();
    }

    public String getMimeType()
    {
        return prototypeEndpoint.getMimeType();
    }

    public Filter getFilter()
    {
        return prototypeEndpoint.getFilter();
    }

    public String getInitialState()
    {
        return prototypeEndpoint.getInitialState();
    }

    public MuleContext getMuleContext()
    {
        return prototypeEndpoint.getMuleContext();
    }

    public String getName()
    {
        return prototypeEndpoint.getName();
    }

    public Map getProperties()
    {
        return prototypeEndpoint.getProperties();
    }

    public Object getProperty(Object key)
    {
        return prototypeEndpoint.getProperty(key);
    }

    public String getProtocol()
    {
        return prototypeEndpoint.getProtocol();
    }

    public int getResponseTimeout()
    {
        return prototypeEndpoint.getResponseTimeout();
    }

    public EndpointMessageProcessorChainFactory getMessageProcessorsFactory()
    {
        return prototypeEndpoint.getMessageProcessorsFactory();
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return prototypeEndpoint.getMessageProcessors();
    }

    public List<MessageProcessor> getResponseMessageProcessors()
    {
        return prototypeEndpoint.getResponseMessageProcessors();
    }

    public EndpointSecurityFilter getSecurityFilter()
    {
        return prototypeEndpoint.getSecurityFilter();
    }

    public TransactionConfig getTransactionConfig()
    {
        return prototypeEndpoint.getTransactionConfig();
    }

    public boolean isDeleteUnacceptedMessages()
    {
        return prototypeEndpoint.isDeleteUnacceptedMessages();
    }

    public boolean isReadOnly()
    {
        return prototypeEndpoint.isReadOnly();
    }

    public MessageExchangePattern getExchangePattern()
    {
        return prototypeEndpoint.getExchangePattern();
    }

    public List<String> getResponseProperties()
    {
        return prototypeEndpoint.getResponseProperties();
    }

    @Override
    public boolean isDynamic()
    {
        return true;
    }

    public String getEndpointBuilderName()
    {
        return prototypeEndpoint.getEndpointBuilderName();
    }

    public boolean isProtocolSupported(String protocol)
    {
        return prototypeEndpoint.isProtocolSupported(protocol);
    }

    public boolean isDisableTransportTransformer()
    {
        return prototypeEndpoint.isDisableTransportTransformer();
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return getStaticEndpoint(event).process(event);
    }

    @Override
    public void setMessagingExceptionHandler(MessagingExceptionHandler messagingExceptionHandler)
    {
        this.exceptionHandler = messagingExceptionHandler;
    }
}
