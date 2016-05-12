/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint.inbound;

import org.mule.runtime.core.PropertyScope;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringUtils;

;


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

    @Override
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
