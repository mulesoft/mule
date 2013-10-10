/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.endpoint;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;

public interface EndpointMessageProcessorChainFactory
{
    public MessageProcessor createInboundMessageProcessorChain(InboundEndpoint endpoint, FlowConstruct flowConstruct, MessageProcessor target) throws MuleException;
    
    public MessageProcessor createOutboundMessageProcessorChain(OutboundEndpoint endpoint, FlowConstruct flowConstruct, MessageProcessor target) throws MuleException;
}


