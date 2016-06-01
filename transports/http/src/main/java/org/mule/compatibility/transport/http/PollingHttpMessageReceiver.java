/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.transport.Connector;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.compatibility.core.transport.AbstractPollingMessageReceiver;
import org.mule.compatibility.transport.http.i18n.HttpMessages;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.MapUtils;
import org.mule.runtime.core.util.StringUtils;

import java.util.Collections;

/**
 * Will poll an http URL and use the response as the input for a service request.
 */
public class PollingHttpMessageReceiver extends AbstractPollingMessageReceiver
{
    protected String etag = null;
    protected boolean checkEtag;
    protected boolean discardEmptyContent;

    //The outbound endpoint to poll
    private OutboundEndpoint outboundEndpoint;

    public PollingHttpMessageReceiver(Connector connector,
                                      FlowConstruct flowConstruct,
                                      final InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        setupFromConnector(connector);
    }

    @Override
    protected boolean pollOnPrimaryInstanceOnly() {
        return true;
    }

    protected void setupFromConnector(Connector connector) throws CreateException
    {
        if (!(connector instanceof HttpPollingConnector))
        {
            throw new CreateException(HttpMessages.pollingReciverCannotbeUsed(), this);
        }

        HttpPollingConnector pollingConnector = (HttpPollingConnector) connector;
        long pollingFrequency = MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency",
                pollingConnector.getPollingFrequency());
        if (pollingFrequency > 0)
        {
            setFrequency(pollingFrequency);
        }

        checkEtag = MapUtils.getBooleanValue(endpoint.getProperties(), "checkEtag", pollingConnector.isCheckEtag());
        discardEmptyContent = MapUtils.getBooleanValue(endpoint.getProperties(), "discardEmptyContent", pollingConnector.isDiscardEmptyContent());
    }

    @Override
    protected void doDispose()
    {
        // nothing to do
    }

    @Override
    protected void doConnect() throws Exception
    {
        // nothing to do
    }

    @Override
    public void doDisconnect() throws Exception
    {
        // nothing to do
    }

    @Override
    public void poll() throws Exception
    {
        MuleContext muleContext = getEndpoint().getMuleContext();

        if (outboundEndpoint == null)
        {
            // We need to create an outbound endpoint to do the polled request using
            // send() as thats the only way we can customize headers and use eTags
            EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(endpoint);
            // Must not use inbound endpoint processors
            endpointBuilder.setMessageProcessors(Collections.<MessageProcessor>emptyList());
            endpointBuilder.setResponseMessageProcessors(Collections.<MessageProcessor>emptyList());
            endpointBuilder.setMessageProcessors(Collections.<MessageProcessor>emptyList());
            endpointBuilder.setResponseMessageProcessors(Collections.<MessageProcessor>emptyList());
            endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);

            outboundEndpoint = getEndpointFactory(muleContext).getOutboundEndpoint(
                    endpointBuilder);
        }

        MuleMessage request = new DefaultMuleMessage(StringUtils.EMPTY, outboundEndpoint.getProperties(), muleContext);
        if (etag != null && checkEtag)
        {
            request.setOutboundProperty(HttpConstants.HEADER_IF_NONE_MATCH, etag);
        }
        request.setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, "GET");

        MuleEvent event = new DefaultMuleEvent(request, outboundEndpoint.getExchangePattern(), flowConstruct);

        MuleEvent result = outboundEndpoint.process(event);
        MuleMessage message = null;
        if (result != null && !VoidMuleEvent.getInstance().equals(result))
        {
            message = result.getMessage();
        }

        final int contentLength = message.getOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, -1);
        if (contentLength == 0 && discardEmptyContent)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Received empty message and ignoring from: " + endpoint.getEndpointURI());
            }
            return;
        }
        int status = message.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        etag = message.getOutboundProperty(HttpConstants.HEADER_ETAG);

        if ((status != HttpConstants.SC_NOT_MODIFIED || !checkEtag))
        {
            routeMessage(message);
        }
    }

    public EndpointFactory getEndpointFactory(MuleContext muleContext)
    {
        return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
    }
}
