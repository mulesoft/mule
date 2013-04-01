/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.ResponseOutputStream;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointMessageProcessorChainFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.CoreMessages;
import org.mule.processor.AbstractRedeliveryPolicy;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

    public static final String DYNAMIC_URI_PLACEHOLDER = "dynamic://endpoint";

    protected transient final Log logger = LogFactory.getLog(DynamicOutboundEndpoint.class);

    private static final long serialVersionUID = 8861985949279708638L;

    /**
     * The URI template used to construct the actual URI to send the message to.
     */
    protected String uriTemplate;

    private final EndpointBuilder builder;

    private final OutboundEndpoint prototypeEndpoint;

    // Caches resolved static endpoints to improve performance
    private Map<String, OutboundEndpoint> staticEndpoints = Collections.synchronizedMap(new LRUMap(64));

    public DynamicOutboundEndpoint(EndpointBuilder builder, String uriTemplate) throws MalformedEndpointException
    {
        validateUriTemplate(uriTemplate);

        this.builder = builder;
        this.uriTemplate = uriTemplate;

        try
        {
            prototypeEndpoint = builder.buildOutboundEndpoint();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    protected void validateUriTemplate(String uri) throws MalformedEndpointException
    {
        if (uri.indexOf(":") > uri.indexOf(ExpressionManager.DEFAULT_EXPRESSION_PREFIX))
        {
            throw new MalformedEndpointException(CoreMessages.dynamicEndpointsMustSpecifyAScheme(), uri);
        }
    }

    protected EndpointURI getEndpointURIForMessage(MuleEvent event) throws DispatchException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Uri before parsing is: " + uriTemplate);
        }

        String newUriString = uriTemplate;
        try
        {
            newUriString = parseURIString(newUriString, event.getMessage());
        }
        catch (final ExpressionRuntimeException e)
        {
            throw new DispatchException(event, this, e);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Uri after parsing is: " + newUriString);
        }

        try
        {
            final MuleEndpointURI resultUri = new MuleEndpointURI(newUriString, getMuleContext());
            resultUri.initialise();

            return resultUri;
        }
        catch (final Exception e)
        {
            throw new DispatchException(CoreMessages.templateCausedMalformedEndpoint(uriTemplate, newUriString), event, this, e);
        }
    }

    protected String parseURIString(String uri, MuleMessage message)
    {
        return this.getMuleContext().getExpressionManager().parse(uri, message, true);
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        EndpointURI endpointURIForMessage = getEndpointURIForMessage(event);
        return getStaticEndpointFor(endpointURIForMessage).process(event);
    }

    private OutboundEndpoint getStaticEndpointFor(EndpointURI uri) throws EndpointException, InitialisationException
    {
        OutboundEndpoint outboundEndpoint = staticEndpoints.get(uri.getAddress());

        if (outboundEndpoint == null)
        {
            outboundEndpoint = createStaticEndpoint(uri);
            staticEndpoints.put(uri.getAddress(), outboundEndpoint);
        }

        return outboundEndpoint;
    }

    private OutboundEndpoint createStaticEndpoint(EndpointURI uri) throws EndpointException, InitialisationException
    {
        try
        {
            EndpointBuilder staticBuilder = (EndpointBuilder) builder.clone();
            staticBuilder.setURIBuilder(new URIBuilder(uri));
            return staticBuilder.buildOutboundEndpoint();
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
        return uriTemplate;
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

    public List<Transformer> getResponseTransformers()
    {
        return prototypeEndpoint.getResponseTransformers();
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

    public List<Transformer> getTransformers()
    {
        return prototypeEndpoint.getTransformers();
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
}
