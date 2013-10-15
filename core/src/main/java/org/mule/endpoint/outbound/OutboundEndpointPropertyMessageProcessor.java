/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint.outbound;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;


/**
 * Sets the outbound endpoint uri on as a property of the message using the following
 * key: {@link MuleProperties#MULE_ENDPOINT_PROPERTY}.
 */
public class OutboundEndpointPropertyMessageProcessor implements MessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        event.getMessage().setOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY,
                                               event.getEndpoint().getEndpointURI().toString());
        event = OptimizedRequestContext.unsafeSetEvent(event);
        return event;
    }
    
    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
