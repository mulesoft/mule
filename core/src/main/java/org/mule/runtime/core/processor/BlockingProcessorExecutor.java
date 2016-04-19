/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.OptimizedRequestContext;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.ProcessorExecutor;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;
import org.mule.runtime.core.routing.MessageFilter;

import java.util.List;

/**
 * This {@link org.mule.runtime.core.api.processor.ProcessorExecutor} implementation executes each {@link org.mule.runtime.core.api.processor.MessageProcessor}
 * in sucession in the same thread until or processors have been invoked or one of the following is returned by a processor:
 * <li>{@link org.mule.runtime.core.VoidMuleEvent}</li>
 * <li><code>null</code></li>
 */
public class BlockingProcessorExecutor implements ProcessorExecutor
{

    protected final MessageProcessorExecutionTemplate messageProcessorExecutionTemplate;
    protected final boolean copyOnVoidEvent;
    protected final List<MessageProcessor> processors;

    protected MuleEvent event;
    private int index;

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
    public final MuleEvent execute() throws MessagingException
    {
        MuleEvent result = event;
        while (hasNext() && isEventValid(event))
        {
            result = executeNext();
            if (!isEventValid(result))
            {
                break;
            }
            event = result;
        }
        return result;
    }

    private boolean isEventValid(MuleEvent result)
    {
        return result != null && !(result instanceof VoidMuleEvent);
    }

    protected boolean hasNext()
    {
        return index < processors.size();
    }

    protected MuleEvent executeNext() throws MessagingException
    {
        MessageProcessor processor = nextProcessor();

        preProcess(processor);

        if (copyOnVoidEvent && processorMayReturnVoidEvent(processor))
        {
            MuleEvent copy = new DefaultMuleEvent(new DefaultMuleMessage(event.getMessage()), event);
            MuleEvent result = messageProcessorExecutionTemplate.execute(processor, event);
            if (isUseEventCopy(result))
            {
                OptimizedRequestContext.unsafeSetEvent(copy);
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
        return !(processor instanceof Component || processor instanceof Transformer
                 || processor instanceof MessageFilter);
        //TODO See MULE-9307 - re-add behaviour when we know if a processor may return null
    }

}
