/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor.chain;

import org.mule.DefaultMuleEvent;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.api.NonBlockingSupported;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageRouter;
import org.mule.api.transport.NonBlockingResponseReplyToHandler;
import org.mule.api.transport.ReplyToHandler;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.api.processor.CompletionHandler;
import org.mule.processor.NonBlockingMessageProcessor;

import java.util.List;

/**
 * Specialized {@link org.mule.processor.chain.ProcessorIterator} that pauses iteration in the case a {@link org.mule.processor.NonBlockingMessageProcessor}
 * is invoked and the flow is executing using non-blocking.  Processor iteration is then continued when the {@link org.mule.processor.NonBlockingMessageProcessor}
 * invokes the {@link org.mule.api.transport.ReplyToHandler}.
 */
public class NonBlockingProcessorIterator extends ProcessorIterator
{

    private ReplyToHandler replyToHandler;

    public NonBlockingProcessorIterator(MuleEvent event, List<MessageProcessor> processors,
                                        MessageProcessorExecutionTemplate executionTemplate, boolean copyOnVoidEvent)
    {
        super(event, processors, executionTemplate, copyOnVoidEvent);
        this.replyToHandler = event.getReplyToHandler();
    }

    @Override
    protected void preProcess(MessageProcessor processor)
    {
        if (event.getExchangePattern().hasResponse() && !event.isSynchronous())
        {
            if ((processor instanceof MessageRouter || processor instanceof InterceptingMessageProcessor) && !
                    (processor instanceof NonBlockingSupported))
            {
                event = new DefaultMuleEvent(event.getMessage(), event, true);
            }

            if (processor instanceof NonBlockingMessageProcessor)
            {
                event = new DefaultMuleEvent(event, new NonBlockingResponseReplyToHandler(new CompletionHandler<MuleEvent, MessagingException>()
                {
                    @Override
                    public void onCompletion(MuleEvent o)
                    {
                        NonBlockingProcessorIterator.this.event = new DefaultMuleEvent(o, replyToHandler);
                        try
                        {
                            resume();
                        }
                        catch (MessagingException e)
                        {
                            onFailure(e);
                        }
                    }

                    @Override
                    public void onFailure(MessagingException exception)
                    {
                        replyToHandler.processExceptionReplyTo(event, exception, null);
                    }
                }));
            }
        }
    }

    private void resume() throws MessagingException
    {
        MuleEvent result = process();
        if (event != null && result != NonBlockingVoidMuleEvent.getInstance())
        {
            try
            {
                replyToHandler.processReplyTo(event, null, null);
            }
            catch (MuleException e)
            {
                replyToHandler.processExceptionReplyTo(event, new MessagingException(event, e), null);
            }
        }
    }

}
