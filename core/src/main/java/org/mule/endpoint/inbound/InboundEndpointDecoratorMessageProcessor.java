/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.inbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.InboundEndpointDecorator;
import org.mule.processor.AbstractInterceptingMessageProcessor;

/**
 * TODO MULE-4872 Remove EndpointDecorator api/usage once customisable inbound
 * message flow is implemented
 */
public class InboundEndpointDecoratorMessageProcessor extends AbstractInterceptingMessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        InboundEndpoint endpoint = (InboundEndpoint) event.getEndpoint();
        // Notify the endpoint of the new message
        if (endpoint instanceof InboundEndpointDecorator)
        {
            if (!((InboundEndpointDecorator) endpoint).onMessage(event.getMessage()))
            {
                return null;
            }
        }
        return processNext(event);
    }
}
