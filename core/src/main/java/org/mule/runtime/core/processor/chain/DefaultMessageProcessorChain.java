/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.RequestReplyReplierMessageProcessor;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.LegacyOutboundEndpoint;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.routing.MessageFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DefaultMessageProcessorChain extends AbstractMessageProcessorChain
{
    protected MessageProcessorExecutionTemplate messageProcessorExecutionTemplate = MessageProcessorExecutionTemplate.createExecutionTemplate();

    protected DefaultMessageProcessorChain(List<MessageProcessor> processors)
    {
        super(null, processors);
    }

    protected DefaultMessageProcessorChain(MessageProcessor... processors)
    {
        super(null, new ArrayList(Arrays.asList(processors)));
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

    @Override
    protected MuleEvent doProcess(MuleEvent event) throws MuleException
    {
        if (event.getMuleContext() != null
            && event.getMuleContext().getConfiguration().isFlowEndingWithOneWayEndpointReturnsNull())
        {
            return doProcessFlowEndingWithOneWayEndpointReturnsNull(event);
        }
        else
        {
            return new ProcessorExecutorFactory().createProcessorExecutor(event, processors,
                                                                          messageProcessorExecutionTemplate, true).execute();
        }
    }

    /*
     * Using old implementation 100% as is.
     */
    private MuleEvent doProcessFlowEndingWithOneWayEndpointReturnsNull(MuleEvent event)
        throws MessagingException
    {
        FlowConstruct flowConstruct = event.getFlowConstruct();
        MuleEvent currentEvent = event;
        MuleEvent resultEvent;
        MuleEvent copy = null;
        Iterator<MessageProcessor> processorIterator = processors.iterator();
        MessageProcessor processor = null;
        if (processorIterator.hasNext())
        {
            processor = processorIterator.next();
        }
        boolean resultWasNull = false;
        while (processor != null)
        {
            MessageProcessor nextProcessor = null;
            if (processorIterator.hasNext())
            {
                nextProcessor = processorIterator.next();
            }

            if (flowConstruct instanceof Flow && nextProcessor != null
                && !(processor instanceof Transformer || processor instanceof MessageFilter || processor instanceof Component
                     || (processor instanceof LegacyOutboundEndpoint && !((LegacyOutboundEndpoint) processor).mayReturnVoidEvent())))
            {
                copy = OptimizedRequestContext.criticalSetEvent(currentEvent);
            }

            resultEvent = messageProcessorExecutionTemplate.execute(processor, currentEvent);

            if (resultWasNull && processor instanceof RequestReplyReplierMessageProcessor)
            {
                // reply-to processing should not resurrect a dead event
                resultEvent = null;
            }

            if (resultEvent != null && !VoidMuleEvent.getInstance().equals(resultEvent))
            {
                resultWasNull = false;
                currentEvent = resultEvent;
            }
            else if (VoidMuleEvent.getInstance().equals(resultEvent))
            {
                if (flowConstruct instanceof Flow && nextProcessor != null)
                {
                    resultWasNull = true;
                    // // In a flow when a MessageProcessor returns null the next
                    // processor acts as an implicit
                    // // branch receiving a copy of the message used for previous
                    // MessageProcessor
                    if (copy != null)
                    {
                        currentEvent = copy;
                    }
                    else
                    {
                        // this should not happen
                        currentEvent = OptimizedRequestContext.criticalSetEvent(currentEvent);
                    }
                }
                else
                {
                    // But in a service we don't do any implicit branching.
                    return VoidMuleEvent.getInstance();
                }
            }
            else if (resultEvent == null)
            {
                return null;
            }
            processor = nextProcessor;
        }
        return currentEvent;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        super.setMuleContext(context);
    }
}
