/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.Connector;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.session.DefaultMuleSession;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.http.i18n.HttpMessages;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;

import java.util.Collections;

/**
 * Will poll an http URL and use the response as the input for a service request.
 */
public class PollingHttpMessageReceiver extends AbstractPollingMessageReceiver
{
    protected String etag = null;
    private boolean checkEtag;
    private boolean discardEmptyContent;
    //The outbound endpoint to poll
    private OutboundEndpoint outboundEndpoint;

    public PollingHttpMessageReceiver(Connector connector,
                                      FlowConstruct flowConstruct,
                                      final InboundEndpoint endpoint) throws CreateException
    {

        super(connector, flowConstruct, endpoint);

        HttpPollingConnector pollingConnector;

        if (connector instanceof HttpPollingConnector)
        {
            pollingConnector = (HttpPollingConnector) connector;
        }
        else
        {
            throw new CreateException(HttpMessages.pollingReciverCannotbeUsed(), this);
        }

        long pollingFrequency = MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency",
                pollingConnector.getPollingFrequency());
        if (pollingFrequency > 0)
        {
            this.setFrequency(pollingFrequency);
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
        MuleContext muleContext = connector.getMuleContext();

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

            outboundEndpoint = muleContext.getEndpointFactory().getOutboundEndpoint(
                    endpointBuilder);
        }

        MuleMessage request = new DefaultMuleMessage(StringUtils.EMPTY, outboundEndpoint.getProperties(), muleContext);
        if (etag != null && checkEtag)
        {
            request.setOutboundProperty(HttpConstants.HEADER_IF_NONE_MATCH, etag);
        }
        request.setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, "GET");

        MuleSession session = new DefaultMuleSession(flowConstruct, connector.getMuleContext());


        MuleEvent event = new DefaultMuleEvent(request, outboundEndpoint, session);

        MuleEvent result = outboundEndpoint.process(event);
        MuleMessage message = null;
        if (result != null)
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
}
