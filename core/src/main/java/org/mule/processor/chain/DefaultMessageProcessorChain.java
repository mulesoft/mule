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

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.construct.SimpleFlowConstruct;

import java.util.Arrays;
import java.util.List;

public class DefaultMessageProcessorChain extends AbstractMessageProcessorChain
{

    protected DefaultMessageProcessorChain(List<MessageProcessor> processors)
    {   
        super(null, processors);
    }

    protected DefaultMessageProcessorChain(MessageProcessor... processors)
    {
        super(null, Arrays.asList(processors));
    }

    protected DefaultMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        super(name, processors);
    }

    protected DefaultMessageProcessorChain(String name, MessageProcessor... processors)
    {
        super(name, Arrays.asList(processors));
    }

    public static MessageProcessorChain from(MessageProcessor messageProcessor)
    {
        return new DefaultMessageProcessorChain(messageProcessor);
    }

    public static MessageProcessorChain from(MessageProcessor... messageProcessors) throws MuleException
    {
        return new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
    }

    public static MessageProcessorChain from(List<MessageProcessor> messageProcessors) throws MuleException
    {
        return new DefaultMessageProcessorChainBuilder().chain(messageProcessors).build();
    }
    
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        FlowConstruct flowConstruct = event.getFlowConstruct();
        MuleEvent currentEvent = event;
        MuleEvent resultEvent;
        for (MessageProcessor processor : processors)
        {
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
