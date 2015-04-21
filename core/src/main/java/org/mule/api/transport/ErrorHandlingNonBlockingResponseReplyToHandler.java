/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.processor.CompletionHandler;

/**
 * Specialized version of {@link org.mule.api.transport.NonBlockingResponseReplyToHandler} that uses a {@link org.mule.api.MessagingException}
 * to handle exceptions.
 */
public class ErrorHandlingNonBlockingResponseReplyToHandler extends NonBlockingResponseReplyToHandler
{

    private MessagingExceptionHandler messagingExceptionHandler;

    public ErrorHandlingNonBlockingResponseReplyToHandler(CompletionHandler completionHandler, MessagingExceptionHandler exceptionHandler)
    {
        super(completionHandler);
        this.messagingExceptionHandler = messagingExceptionHandler;
    }

    @Override
    public void processExceptionReplyTo(MuleEvent event, MessagingException exception, Object replyTo)
    {
        MuleEvent result;
        if (messagingExceptionHandler != null)
        {
            result = messagingExceptionHandler.handleException(exception, exception.getEvent());
        }
        else
        {
            result = exception.getEvent().getFlowConstruct().getExceptionListener().handleException(exception, event);
        }
        exception.setProcessedEvent(result);
        if (!exception.handled())
        {
            super.processExceptionReplyTo(event, exception, replyTo);
        }
        else
        {
            completionHandler.onCompletion(exception.getEvent());
        }
    }
}
