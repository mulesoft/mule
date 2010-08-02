/*
 * $Id: DynamicOutboundEndpoint.java 173 2009-11-07 19:41:52Z ross $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An Outbound endpoint who's URI will be constructed based on the current message. This allows for the destination of a message to change
 * based on the contents of the message.  Note that this endpoint ONLY substitutes the URI, but other config elements such as
 * the connector (and scheme), transformers, filters, etc do not change.  You cannot change an endpoint scheme dynamically so you
 * can't switch between HTTP and JMS for example using the same dynamic endpoint.
 */
public class DynamicOutboundEndpoint extends DynamicURIOutboundEndpoint
{
    public static final String DYNAMIC_URI_PLACEHOLDER = "dynamic://endpoint";

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(DynamicOutboundEndpoint.class);

    private static final long serialVersionUID = 8861985949279708638L;

    /**
     * THe URI template used to construct the actual URI to send the message to.
     */
    protected String uriTemplate;

    private EndpointBuilder builder;

    public DynamicOutboundEndpoint(MuleContext muleContext, EndpointBuilder builder, String uriTemplate) throws MalformedEndpointException
    {
        super(new NullOutboundEndpoint(muleContext));
        this.builder = builder;
        this.uriTemplate = uriTemplate;
        validateUriTemplate(uriTemplate);
    }

    protected void validateUriTemplate(String uri) throws MalformedEndpointException
    {
        if (uri.indexOf(":") > uri.indexOf(ExpressionManager.DEFAULT_EXPRESSION_PREFIX))
        {
            throw new MalformedEndpointException(CoreMessages.dynamicEndpointsMustSpecifyAScheme(), uri);
        }
    }

    protected EndpointURI getEndpointURIForMessage(MuleMessage message) throws DispatchException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Uri before parsing is: " + uriTemplate);
        }

        String newUriString = uriTemplate;
        try
        {
            newUriString = parseURIString(newUriString, message);
        }
        catch (ExpressionRuntimeException e)
        {
            throw new DispatchException(message, this, e);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Uri after parsing is: " + newUriString);
        }

        try
        {
            MuleEndpointURI uri = new MuleEndpointURI(newUriString, getMuleContext());

            setEndpointURI(uri);

            getEndpointURI().initialise();
            return getEndpointURI();
        }
        catch (Exception e)
        {
            throw new DispatchException(
                    CoreMessages.templateCausedMalformedEndpoint(uriTemplate, newUriString),
                    message, this, e);
        }

    }

    protected String parseURIString(String uri, MuleMessage message)
    {
        return this.getMuleContext().getExpressionManager().parse(uri, message, true);
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        EndpointURI uri = getEndpointURIForMessage(event.getMessage());
        if (endpoint instanceof NullOutboundEndpoint)
        {
            builder.setURIBuilder(new URIBuilder(uri));
            endpoint = builder.buildOutboundEndpoint();
        }
        OutboundEndpoint outboundEndpoint = new DynamicURIOutboundEndpoint(endpoint, uri);

        return outboundEndpoint.process(event);
    }


    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof DynamicOutboundEndpoint))
        {
            return false;
        }

        DynamicOutboundEndpoint that = (DynamicOutboundEndpoint) o;
        if (uriTemplate != null ? !uriTemplate.equals(that.uriTemplate) : that.uriTemplate != null)
        {
            return false;
        }
        return false;
    }

    @Override
    public int hashCode()
    {
        return new Double(Math.random()).intValue();
    }

    static Connector createDynamicConnector(MuleContext muleContext)
    {
        try
        {
            return new TransportFactory(muleContext).createConnector(DYNAMIC_URI_PLACEHOLDER);
        }
        catch (TransportFactoryException e)
        {
            //This should never happen
            throw new MuleRuntimeException(e);
        }
    }

    protected static class NullOutboundEndpoint extends AbstractEndpoint implements OutboundEndpoint
    {
        NullOutboundEndpoint(MuleContext muleContext)
        {
            super(createDynamicConnector(muleContext), null, null, new HashMap(), null, true, MessageExchangePattern.ONE_WAY, 0, "started", null, null, muleContext, null, null, null, null, true, null);
        }

        @Override
        protected MessageProcessor createMessageProcessorChain() throws MuleException
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
    }

}
