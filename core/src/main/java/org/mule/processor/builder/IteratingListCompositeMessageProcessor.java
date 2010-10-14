/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor.builder;

import org.mule.DefaultMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.processor.chain.AbstractMessageProcessorChain;

import java.util.Arrays;
import java.util.List;

public class IteratingListCompositeMessageProcessor extends AbstractMessageProcessorChain
{

    public IteratingListCompositeMessageProcessor(List<MessageProcessor> processors)
    {
        super(null, processors);
    }

    public IteratingListCompositeMessageProcessor(MessageProcessor... processors)
    {
        super(null, Arrays.asList(processors));
    }

    public IteratingListCompositeMessageProcessor(String name, List<MessageProcessor> processors)
    {
        super(name, processors);
    }

    public IteratingListCompositeMessageProcessor(String name, MessageProcessor... processors)
    {
        super(name, Arrays.asList(processors));
    }

    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        FlowConstruct flowConstruct = event.getFlowConstruct();
        MuleEvent currentEvent = event;
        for (MessageProcessor processor : processors)
        {
            // If the next message processor is an outbound router then create
            // outbound event
            if (processor instanceof OutboundEndpoint)
            {
                currentEvent = new DefaultMuleEvent(currentEvent.getMessage(), (OutboundEndpoint) processor,
                    currentEvent.getSession());
            }
            currentEvent = processor.process(currentEvent);
            if (currentEvent == null)
            {
                if (flowConstruct instanceof SimpleFlowConstruct)
                {
                    currentEvent = processNext(OptimizedRequestContext.criticalSetEvent(currentEvent));
                }
                else
                {
                    return null;
                }
            }
        }
        return currentEvent;
    }

}
