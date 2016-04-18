/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.NonBlockingSupported;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorContainer;
import org.mule.api.transport.NonBlockingReplyToHandler;
import org.mule.api.transport.ReplyToHandler;
import org.mule.execution.MessageProcessorExecutionTemplate;

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
    private final MessageExchangePattern messageExchangePattern;

    public NonBlockingProcessorExecutor(MuleEvent event, List<MessageProcessor> processors,
                                        MessageProcessorExecutionTemplate executionTemplate, boolean copyOnVoidEvent)
    {
        super(event, processors, executionTemplate, copyOnVoidEvent);
        this.replyToHandler = event.getReplyToHandler();
        this.messageExchangePattern = event.getExchangePattern();
    }

    @Override
    protected void preProcess(MessageProcessor processor)
    {
        if (event.isAllowNonBlocking())
        {
            if (!processorSupportsNonBlocking(processor))
            {
                logger.info("The message processor {} does not currently support non-blocking execution and " +
                            "processing will now fall back to blocking.  The 'non-blocking' processing strategy is " +
                            "not recommended if unsupported message processors are being used.  ", processor.getClass());
                // Make event synchronous so that non-blocking is not used
                event = new DefaultMuleEvent(event, event.getFlowConstruct(), event.getReplyToHandler(), event.getReplyToDestination(), true);
                // Update RequestContext ThreadLocal for backwards compatibility
                OptimizedRequestContext.unsafeSetEvent(event);
            }

            if (processor instanceof NonBlockingMessageProcessor)
            {
                // Even if there is no ReplyToHandler available, using non-blocking processing anyway for a non-blocking
                // message processor if a response isn't required.
                if (!(messageExchangePattern.hasResponse() && replyToHandler == null))
                {
                    event = new DefaultMuleEvent(event, new NonBlockingProcessorExecutorReplyToHandler());
                    // Update RequestContext ThreadLocal for backwards compatibility
                    OptimizedRequestContext.unsafeSetEvent(event);
                }
            }
        }
    }

    private boolean processorSupportsNonBlocking(MessageProcessor processor)
    {
        if (processor instanceof NonBlockingSupported)
        {
            return true;
        }
        else if (processor instanceof AsyncInterceptingMessageProcessor && !messageExchangePattern.hasResponse())
        {
            return true;
        }
        else
        {
            return !(processor instanceof MessageProcessorContainer || processor instanceof InterceptingMessageProcessor);
        }
    }

    private void resume(final MuleEvent event) throws MuleException
    {
        this.event = recreateEventWithOriginalReplyToHandler(event);

        MuleEvent result = execute();
        if (!(result instanceof NonBlockingVoidMuleEvent) && replyToHandler != null)
        {
            replyToHandler.processReplyTo(result, null, null);
        }
    }

    private MuleEvent recreateEventWithOriginalReplyToHandler(MuleEvent event)
    {
        if (event != null)
        {
            event = new DefaultMuleEvent(event, replyToHandler);
            // Update RequestContext ThreadLocal for backwards compatibility
            OptimizedRequestContext.unsafeSetEvent(event);
        }
        return event;
    }

    class NonBlockingProcessorExecutorReplyToHandler implements NonBlockingReplyToHandler
    {

        @Override
        public void processReplyTo(final MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
        {
            try
            {
                resume(event);
            }
            catch (Throwable e)
            {
                if (e instanceof MessagingException)
                {
                    processExceptionReplyTo((MessagingException) e, replyTo);
                }
                else
                {
                    processExceptionReplyTo(new MessagingException(event, e), replyTo);
                }
            }
        }

        @Override
        public void processExceptionReplyTo(MessagingException exception, Object replyTo)
        {
            if (replyToHandler != null)
            {
                replyToHandler.processExceptionReplyTo(exception, replyTo);
            }
            else
            {
                event.getFlowConstruct().getExceptionListener().handleException(exception, exception.getEvent());
            }
        }
    }
}
