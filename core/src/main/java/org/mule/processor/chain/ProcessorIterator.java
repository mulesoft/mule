/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.MessageExchangePattern;
import org.mule.OptimizedRequestContext;
import org.mule.VoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.component.Component;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.NonBlockingResponseReplyToHandler;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.processor.AbstractInterceptingMessageProcessorBase;
import org.mule.routing.MessageFilter;

import java.util.List;

/**
 * Iterates over a list of {@link org.mule.api.processor.MessageProcessor}'s processing them one by one using the result
 * of the first processor to invoke the second and so on.  MessageProcessor implementations aside from simply iterating
 * over processors implement rules regarding if and when iteration should stop early or even stop temporarily and be
 * continued later.
 * <p/>
 * This implementation processes each {@link org.mule.api.processor.MessageProcessor} in sucession in the same thread
 * until or processors have been invoked or one of the following is returned by a processor:
 * <li>{@link org.mule.VoidMuleEvent}</li>
 * <li><code>null</code></li>
 */
public class ProcessorIterator
{

    final protected MessageProcessorExecutionTemplate messageProcessorExecutionTemplate;
    final protected boolean copyOnVoidEvent;
    final protected List<MessageProcessor> processors;

    protected volatile int index;
    protected MuleEvent event;

    public ProcessorIterator(MuleEvent event, List<MessageProcessor> processors,
                             MessageProcessorExecutionTemplate messageProcessorExecutionTemplate, boolean copyOnVoidEvent)
    {
        this.event = event;
        this.processors = processors;
        this.copyOnVoidEvent = copyOnVoidEvent;
        this.messageProcessorExecutionTemplate = messageProcessorExecutionTemplate;
    }

    public MuleEvent process() throws MessagingException
    {
        while (continueProcessing())
        {
            event = processNext();
        }
        return event;
    }

    protected boolean continueProcessing()
    {
        return index < processors.size() && event != null && !VoidMuleEvent.isVoid(event);
    }

    protected MuleEvent processNext() throws MessagingException
    {
        MessageProcessor processor = nextProcessor();

        preProcess(processor);

        if (copyOnVoidEvent && processorMayReturnVoidEvent(processor))
        {
            MuleEvent copy = OptimizedRequestContext.criticalSetEvent(event);
            MuleEvent result = messageProcessorExecutionTemplate.execute(processor, event);
            if (VoidMuleEvent.getInstance().equals(result))
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

    protected void preProcess(MessageProcessor processor)
    {
        // template method.
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

    public static ProcessorIterator getProcessorIterator(MuleEvent event,
                                                         List<MessageProcessor> processors,
                                                         MessageProcessorExecutionTemplate executionTemplate,
                                                         boolean copyOnVoidEvent)
    {
        if (event.getExchangePattern().hasResponse() && !event.isSynchronous() && event.getReplyToHandler()
                instanceof NonBlockingResponseReplyToHandler)
        {
            return new NonBlockingProcessorIterator(event, processors, executionTemplate, copyOnVoidEvent);
        }
        else if (event.getFlowConstruct() instanceof Service)
        {
            return new ServiceProcessorIterator(event, processors, executionTemplate, copyOnVoidEvent);
        }
        else
        {
            return new ProcessorIterator(event, processors, executionTemplate, copyOnVoidEvent);
        }
    }

}
