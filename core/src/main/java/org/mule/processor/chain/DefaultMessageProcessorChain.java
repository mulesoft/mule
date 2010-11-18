/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.processor.chain;

import org.mule.DefaultMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.routing.WireTap;

import java.util.Arrays;
import java.util.List;

public class DefaultMessageProcessorChain extends AbstractMessageProcessorChain
{

    public DefaultMessageProcessorChain(List<MessageProcessor> processors)
    {
        super(null, processors);
    }

    public DefaultMessageProcessorChain(MessageProcessor... processors)
    {
        super(null, Arrays.asList(processors));
    }

    public DefaultMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        super(name, processors);
    }

    public DefaultMessageProcessorChain(String name, MessageProcessor... processors)
    {
        super(name, Arrays.asList(processors));
    }

    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        FlowConstruct flowConstruct = event.getFlowConstruct();
        MuleEvent currentEvent = event;
        MuleEvent resultEvent;
        for (MessageProcessor processor : processors)
        {
            final WireTap wireTap = getCallbackMap().get(processor);
            if (wireTap != null)
            {
                event = wireTap.process(event);
            }
            // If the next message processor is an outbound router then create
            // outbound event
            if (processor instanceof OutboundEndpoint)
            {
                currentEvent = new DefaultMuleEvent(currentEvent.getMessage(), (OutboundEndpoint) processor,
                    currentEvent.getSession());
            }
            resultEvent = processor.process(currentEvent);
            if (resultEvent != null)
            {
                currentEvent = resultEvent;
            }
            else
            {
                if (flowConstruct instanceof SimpleFlowConstruct)
                {
                    currentEvent = OptimizedRequestContext.criticalSetEvent(currentEvent);
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
