/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.api.NonBlockingSupported;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageRouter;
import org.mule.api.transport.CompletionHandlerReplyToHandlerAdaptor;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.api.CompletionHandler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Specialized {@link org.mule.processor.BlockingProcessorExecutor} that pauses iteration in the case a {@link org
 * .mule.processor.NonBlockingMessageProcessor}
 * is invoked and the flow is executing using non-blocking.  Processor execution is then continued when the {@link
 * org.mule.processor.NonBlockingMessageProcessor}
 * invokes the {@link org.mule.api.transport.ReplyToHandler}.
 */
public class NonBlockingProcessorExecutor extends BlockingProcessorExecutor
{

    private static final Logger logger = LoggerFactory.getLogger(NonBlockingProcessorExecutor.class);
    private final ReplyToHandler replyToHandler;

    public NonBlockingProcessorExecutor(MuleEvent event, List<MessageProcessor> processors,
                                        MessageProcessorExecutionTemplate executionTemplate, boolean copyOnVoidEvent)
    {
        super(event, processors, executionTemplate, copyOnVoidEvent);
        this.replyToHandler = event.getReplyToHandler();
    }

    @Override
    protected void preProcess(MessageProcessor processor)
    {
        if (event.isAllowNonBlocking() && replyToHandler != null)
        {
            if ((processor instanceof MessageRouter || processor instanceof InterceptingMessageProcessor) && !
                    (processor instanceof NonBlockingSupported))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("The message processor {} does not currenlty support non-blocking.  Floe execution " +
                                 "will now fall back to blocking mode", processor);
                }
                event = new DefaultMuleEvent(event.getMessage(), event, true);
            }

            if (processor instanceof NonBlockingMessageProcessor)
            {
                event = new DefaultMuleEvent(event, new CompletionHandlerReplyToHandlerAdaptor(new CompletionHandler<MuleEvent, MessagingException>()
                {
                    @Override
                    public void onCompletion(MuleEvent event)
                    {
                        NonBlockingProcessorExecutor.this.event = new DefaultMuleEvent(event, replyToHandler);
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
        MuleEvent result = execute();
        if (result != null && !(result instanceof VoidMuleEvent))
        {
            try
            {
                replyToHandler.processReplyTo(result, null, null);
            }
            catch (MuleException e)
            {
                replyToHandler.processExceptionReplyTo(event, new MessagingException(event, e), null);
            }
        }
    }

}
