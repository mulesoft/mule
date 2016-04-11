/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;

/**
 * Implementation of {@link DefaultMessageProcessorChainBuilder} that injects the
 * {@link org.mule.api.endpoint.OutboundEndpoint} instance into message processors that implement {@link EndpointAware}
 */
public class OutboundEndpointMessageProcessorChainBuilder extends DefaultMessageProcessorChainBuilder
{

    protected ImmutableEndpoint endpoint;

    public OutboundEndpointMessageProcessorChainBuilder(ImmutableEndpoint endpoint, FlowConstruct flowConstruct)
    {
        super(flowConstruct);
        this.endpoint = endpoint;
    }

    public OutboundEndpointMessageProcessorChainBuilder(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    @Override
    protected MessageProcessor initializeMessageProcessor(Object processor) throws MuleException
    {
        if (processor instanceof EndpointAware)
        {
            ((EndpointAware) processor).setEndpoint(endpoint);
        }
        return super.initializeMessageProcessor(processor);
    }

}
