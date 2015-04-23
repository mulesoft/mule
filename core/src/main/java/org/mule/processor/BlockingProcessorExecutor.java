/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.MessageExchangePattern;
import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.component.Component;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.ProcessorExecutor;
import org.mule.api.transformer.Transformer;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.routing.MessageFilter;

import java.util.List;

/**
 * This {@link org.mule.api.processor.ProcessorExecutor} implementation executes each {@link org.mule.api.processor.MessageProcessor}
 * in sucession in the same thread until or processors have been invoked or one of the following is returned by a processor:
 * <li>{@link org.mule.VoidMuleEvent}</li>
 * <li><code>null</code></li>
 */
public class BlockingProcessorExecutor implements ProcessorExecutor
{

    protected final MessageProcessorExecutionTemplate messageProcessorExecutionTemplate;
    protected final boolean copyOnVoidEvent;
    protected final List<MessageProcessor> processors;

    protected volatile int index;
    protected MuleEvent event;

    public BlockingProcessorExecutor(MuleEvent event, List<MessageProcessor> processors,
                                     MessageProcessorExecutionTemplate messageProcessorExecutionTemplate, boolean
            copyOnVoidEvent)
    {
        this.event = event;
        this.processors = processors;
        this.copyOnVoidEvent = copyOnVoidEvent;
        this.messageProcessorExecutionTemplate = messageProcessorExecutionTemplate;
    }

    @Override
    public MuleEvent execute() throws MessagingException
    {
        while (continueExecuting())
        {
            event = executeNext();
        }
        return event;
    }

    protected boolean continueExecuting()
    {
        return index < processors.size() && event != null && !(event instanceof VoidMuleEvent);
    }

    protected MuleEvent executeNext() throws MessagingException
    {
        MessageProcessor processor = nextProcessor();

        preProcess(processor);

        if (copyOnVoidEvent && processorMayReturnVoidEvent(processor))
        {
            MuleEvent copy = OptimizedRequestContext.criticalSetEvent(event);
            MuleEvent result = messageProcessorExecutionTemplate.execute(processor, event);
            if (isUseEventCopy(result))
            {
                result = copy;
            }
            return result;
        }
        else
        {
            return messageProcessorExecutionTemplate.execute(processor, event);
        }
    }

    protected boolean isUseEventCopy(MuleEvent result)
    {
        return VoidMuleEvent.getInstance().equals(result);
    }

    protected void preProcess(MessageProcessor processor)
    {
    }

    protected MessageProcessor nextProcessor()
    {
        return processors.get(index++);
    }

    private boolean processorMayReturnVoidEvent(MessageProcessor processor)
    {
        if (processor instanceof OutboundEndpoint)
        {
            MessageExchangePattern exchangePattern = ((OutboundEndpoint) processor).getExchangePattern();
            return exchangePattern == null ? true : !exchangePattern.hasResponse();
        }
        else if (processor instanceof Component || processor instanceof Transformer
                 || processor instanceof MessageFilter)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

}
