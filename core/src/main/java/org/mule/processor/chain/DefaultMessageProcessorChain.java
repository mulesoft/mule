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
import org.mule.construct.SimpleFlowConstruct;

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
