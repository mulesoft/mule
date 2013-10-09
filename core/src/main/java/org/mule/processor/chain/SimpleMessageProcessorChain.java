/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor.chain;

import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

import java.util.List;

public class SimpleMessageProcessorChain extends DefaultMessageProcessorChain
{
    public SimpleMessageProcessorChain(List<MessageProcessor> processors)
    {
        super(processors);
    }

    public SimpleMessageProcessorChain(MessageProcessor... processors)
    {
        super(processors);
    }

    public SimpleMessageProcessorChain(String name, List<MessageProcessor> processors)
    {
        super(name, processors);
    }

    public SimpleMessageProcessorChain(String name, MessageProcessor... processors)
    {
        super(name, processors);
    }

    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        for (int i = 0; i < processors.size(); i++)
        {
            MessageProcessor processor = processors.get(i);
            event = messageProcessorExecutionTemplate.execute(processor, event);
            if (event == null)
            {
                return null;
            }
            else if (VoidMuleEvent.getInstance().equals(event))
            {
                return event;
            }
        }
        return event;
    }

}
