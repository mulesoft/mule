/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transport;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.ProcessorExecutor;
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.api.CompletionHandler;
import org.mule.processor.NonBlockingProcessorExecutor;
import org.mule.processor.chain.ProcessorExecutorFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * {@link CompletionHandlerReplyToHandlerAdaptor} processes the Flow response phase when the
 * {@link org.mule.processor.strategy.NonBlockingProcessingStrategy} is being used.
 * <p/>
 * This {@link org.mule.api.transport.ReplyToHandler} processes any {@link org.mule.api.processor.MessageProcessor} that
 * have been added to be executed as part of the response phase before invoking the {@link org.mule.api
 * .CompletionHandler}
 * provided.  In the case of an exception, response processors will not be invoked and instead {@link org.mule.api
 * .CompletionHandler#onFailure(Object)}
 * invoked directly.
 */
public class CompletionHandlerReplyToHandlerAdaptor implements ReplyToHandler
{

    protected final CompletionHandler completionHandler;

    public CompletionHandlerReplyToHandlerAdaptor(final CompletionHandler completionHandler)
    {
        this.completionHandler = completionHandler;
    }

    @Override
    public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws
                                                                                           MuleException
    {
        completionHandler.onCompletion(event);
    }

    @Override
    public void processExceptionReplyTo(MuleEvent event, MessagingException exception, Object replyTo)
    {
        completionHandler.onFailure(exception);
    }

}