/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.chain.SimpleMessageProcessorChainBuilder;

public class EndpointMessageProcessorChainBuilder extends SimpleMessageProcessorChainBuilder
{

    protected ImmutableEndpoint endpoint;

    public EndpointMessageProcessorChainBuilder(ImmutableEndpoint endpoint, FlowConstruct flowConstruct)
    {
        super(flowConstruct);
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
