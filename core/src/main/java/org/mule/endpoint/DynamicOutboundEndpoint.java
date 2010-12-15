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

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.expression.ExpressionRuntimeException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.Connector;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.service.TransportFactory;
import org.mule.transport.service.TransportFactoryException;

/**
 * An Outbound endpoint who's URI will be constructed based on the current message.
 * This allows for the destination of a message to change based on the contents of
 * the message. Note that this endpoint ONLY substitutes the URI, but other config
 * elements such as the connector (and scheme), transformers, filters, etc do not
 * change. You cannot change an endpoint scheme dynamically so you can't switch
 * between HTTP and JMS for example using the same dynamic endpoint.
 */
public class DynamicOutboundEndpoint extends DynamicURIOutboundEndpoint
{
    public static final String DYNAMIC_URI_PLACEHOLDER = "dynamic://endpoint";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DynamicOutboundEndpoint.class);

    private static final long serialVersionUID = 8861985949279708638L;

    /**
     * The URI template used to construct the actual URI to send the message to.
     */
    protected String uriTemplate;

    private final EndpointBuilder builder;

    public DynamicOutboundEndpoint(MuleContext muleContext, EndpointBuilder builder, String uriTemplate)
        throws MalformedEndpointException
    {
        super(new NullOutboundEndpoint(muleContext, builder));
        this.builder = builder;
        this.uriTemplate = uriTemplate;
        validateUriTemplate(uriTemplate);
    }

    @Override
    public String getAddress()
    {
        final EndpointURI uri = getEndpointURI();
        if (uri != null)
        {
            return uri.getUri().toString();
        }
        else
        {
            return uriTemplate;
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
            final MuleEndpointURI uri = new MuleEndpointURI(newUriString, getMuleContext());
            uri.initialise();
            setEndpointURI(uri);
            return getEndpointURI();
        }
        catch (final Exception e)
        {
            throw new DispatchException(CoreMessages.templateCausedMalformedEndpoint(uriTemplate,
                newUriString), event, this, e);
        }

    }

    protected String parseURIString(String uri, MuleMessage message)
    {
        return this.getMuleContext().getExpressionManager().parse(uri, message, true);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        final EndpointURI uri = getEndpointURIForMessage(event);
        if (endpoint instanceof NullOutboundEndpoint)
        {
            builder.setURIBuilder(new URIBuilder(uri));
            endpoint = builder.buildOutboundEndpoint();
        }
        final OutboundEndpoint outboundEndpoint = new DynamicURIOutboundEndpoint(endpoint, uri);

        return outboundEndpoint.process(event);
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

    protected static class NullOutboundEndpoint extends AbstractEndpoint implements OutboundEndpoint
    {
        private static final long serialVersionUID = 7927987219248986540L;

        NullOutboundEndpoint(MuleContext muleContext, EndpointBuilder builder)
        {
            super(createDynamicConnector(muleContext), null, null, Collections.emptyMap(), null, true,
                getMessageExchangePattern(builder), 0, "started", null, null, muleContext, null, null, null,
                null, true, null);
        }

        @Override
        protected MessageProcessor createMessageProcessorChain(FlowConstruct flowConstruct)
            throws MuleException
        {
            throw new UnsupportedOperationException("createMessageProcessorChain");
        }

        public List<String> getResponseProperties()
        {
            return Collections.emptyList();
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new UnsupportedOperationException("process");
        }

        static Connector createDynamicConnector(MuleContext muleContext)
        {
            try
            {
                return new TransportFactory(muleContext).createConnector(DYNAMIC_URI_PLACEHOLDER);
            }
            catch (final TransportFactoryException e)
            {
                // This should never happen
                throw new MuleRuntimeException(e);
            }
        }

        static MessageExchangePattern getMessageExchangePattern(EndpointBuilder builder)
        {
            if (!(builder instanceof AbstractEndpointBuilder))
            {
                return MessageExchangePattern.ONE_WAY;
            }

            return ((AbstractEndpointBuilder) builder).messageExchangePattern;
        }
    }

}
