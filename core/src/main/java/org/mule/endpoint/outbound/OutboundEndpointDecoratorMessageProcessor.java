/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint.outbound;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpointDecorator;
import org.mule.processor.AbstractInterceptingMessageProcessor;

/**
 * TODO MULE-4872 Remove EndpointDecorator api/usage once customisable inbound
 * message flow is implemented
 */
public class OutboundEndpointDecoratorMessageProcessor extends AbstractInterceptingMessageProcessor
{
    private OutboundEndpointDecorator endpoint;

    public OutboundEndpointDecoratorMessageProcessor(OutboundEndpointDecorator endpoint)
    {
        this.endpoint = endpoint;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (!endpoint.onMessage(event.getMessage()))
        {
            return null;
        }
        else
        {
            return processNext(event);
        }
    }
}
