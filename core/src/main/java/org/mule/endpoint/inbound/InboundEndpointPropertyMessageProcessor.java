/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.inbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.util.ObjectUtils;
import org.mule.util.StringUtils;


/**
 * Sets the inbound endpoint uri on as a property of the message using the following
 * key: {@link MuleProperties#MULE_ORIGINATING_ENDPOINT_PROPERTY}.
 */
public class InboundEndpointPropertyMessageProcessor implements MessageProcessor
{
    private InboundEndpoint endpoint;

    public InboundEndpointPropertyMessageProcessor(InboundEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        // If the endpoint has a logical name, use it, otherwise use the URI.
        String inboundEndpoint = endpoint.getName();

        if (StringUtils.isBlank(inboundEndpoint))
        {
            // URI
            inboundEndpoint = endpoint.getEndpointURI().getUri().toString();
        }
        event.getMessage().setProperty(MuleProperties.MULE_ORIGINATING_ENDPOINT_PROPERTY, inboundEndpoint, PropertyScope.INBOUND);
        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
