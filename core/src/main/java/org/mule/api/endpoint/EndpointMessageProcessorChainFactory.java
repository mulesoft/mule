/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.endpoint;

import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;

public interface EndpointMessageProcessorChainFactory
{
    public MessageProcessor createInboundMessageProcessorChain(InboundEndpoint endpoint, FlowConstruct flowConstruct, MessageProcessor target) throws MuleException;
    
    public MessageProcessor createOutboundMessageProcessorChain(OutboundEndpoint endpoint, MessageProcessor target) throws MuleException;
}


