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
import org.mule.util.StringUtils;

import java.util.Iterator;
import java.util.List;

public class IteratingCompositeMessageProcessor implements MessageProcessor
{

    protected List<MessageProcessor> processors;

    public IteratingCompositeMessageProcessor(List<MessageProcessor> processors)
    {
        this.processors = processors;
    }

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        FlowConstruct flowConstruct = event.getFlowConstruct();
        MuleEvent currentEvent = event;
        MuleEvent resultEvent;
        for (MessageProcessor processor : processors)
        {
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

    @Override
    public String toString()
    {
        StringBuilder string = new StringBuilder();
        string.append(getClass().getSimpleName());

        Iterator<MessageProcessor> mpIterator = processors.iterator();

        final String nl = String.format("%n");

        // TODO have it print the nested structure with indents increasing for nested MPCs
        if (mpIterator.hasNext())
        {
            string.append(String.format("%n[ "));
            while (mpIterator.hasNext())
            {
                MessageProcessor mp = mpIterator.next();
                final String indented = StringUtils.replace(mp.toString(), nl, String.format("%n  "));
                string.append(String.format("%n  %s", indented));
                if (mpIterator.hasNext())
                {
                    string.append(", ");
                }
            }
            string.append(String.format("%n]"));
        }

        return string.toString();
    }
}
