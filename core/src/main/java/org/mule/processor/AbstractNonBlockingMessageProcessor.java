/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.NonBlockingVoidMuleEvent;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.CompletionHandler;
import org.mule.api.transport.ReplyToHandler;

/**
 * Abstract implementation of {@link org.mule.processor.NonBlockingMessageProcessor} that determines if processing should
 * be performed blocking or non-blocking and defines two template methods for each mode of processing.
 */
public abstract class AbstractNonBlockingMessageProcessor implements NonBlockingMessageProcessor
{

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (isNonBlocking(event))
        {
            processNonBlocking(event, new NonBlockingCompletionHandler(event));
            return NonBlockingVoidMuleEvent.getInstance();
        }
        else
        {
            return processBlocking(event);
        }
    }

    public boolean isNonBlocking(MuleEvent event)
    {
        return event.getExchangePattern().hasResponse() && !event.isSynchronous() && event.getReplyToHandler() != null;
    }

    abstract protected void processNonBlocking(MuleEvent event, NonBlockingCompletionHandler completionHandler) throws MuleException;

    abstract protected MuleEvent processBlocking(MuleEvent event) throws MuleException;

    protected class NonBlockingCompletionHandler implements CompletionHandler<MuleEvent, MessagingException>
    {

        private MuleEvent event;
        private ReplyToHandler replyToHandler;

        public NonBlockingCompletionHandler(MuleEvent event)
        {
            this.event = event;
            this.replyToHandler = event.getReplyToHandler();
        }

        public void onFailure(final MessagingException exception)
        {
            replyToHandler.processExceptionReplyTo(event, exception, null);
        }

        public void onCompletion(MuleEvent event)
        {
            try
            {
                replyToHandler.processReplyTo(event, null, null);
            }
            catch (Exception e)
            {
                this.event.getFlowConstruct().getExceptionListener().handleException(e, event);
            }
        }
    }
}
