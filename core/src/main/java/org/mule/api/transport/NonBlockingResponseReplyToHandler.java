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
import org.mule.execution.MessageProcessorExecutionTemplate;
import org.mule.api.processor.CompletionHandler;
import org.mule.processor.chain.NonBlockingProcessorIterator;
import org.mule.processor.chain.ProcessorIterator;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * {@link org.mule.api.transport.NonBlockingResponseReplyToHandler} is used to process the Flow response phase when the
 * {@link org.mule.processor.strategy.NonBlockingProcessingStrategy} is being used.
 *
 * This {@link org.mule.api.transport.ReplyToHandler} processes any {@link org.mule.api.processor.MessageProcessor} that
 * have been added to be executed as part of the response phase before invoking the {@link org.mule.api.processor.CompletionHandler}
 * provided.  In the case of an exception, response processors will not be invoked and instead {@link org.mule.api.processor.CompletionHandler#onFailure(Object)}
 * invoked directly.
 */
public class NonBlockingResponseReplyToHandler implements ReplyToHandler
{

    protected final CompletionHandler completionHandler;

    private MessageProcessorExecutionTemplate messageProcessorExecutionTemplate = MessageProcessorExecutionTemplate
            .createExecutionTemplate();
    private List<MessageProcessor> responseProcessors = Collections.synchronizedList(new LinkedList<MessageProcessor>());

    public NonBlockingResponseReplyToHandler(final CompletionHandler completionHandler)
    {
        this.completionHandler = completionHandler;
        responseProcessors.add(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                completionHandler.onCompletion(event);
                return null;
            }
        });
    }

    public synchronized void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws
                                                                                           MuleException
    {
        ProcessorIterator iterator = new ReplyToNonBlockingProcessorIterator(event, null, responseProcessors,
                                                                             messageProcessorExecutionTemplate);
        try
        {
            iterator.process();
        }
        catch (MessagingException e)
        {
            completionHandler.onFailure(e);
        }
    }

    public void processExceptionReplyTo(MuleEvent event, MessagingException exception, Object replyTo)
    {
        completionHandler.onFailure(exception);
    }

    public void addResponseMessageProcessor(MessageProcessor mp)
    {
        responseProcessors.add(0, mp);
    }

    class ReplyToNonBlockingProcessorIterator extends NonBlockingProcessorIterator
    {

        public ReplyToNonBlockingProcessorIterator(MuleEvent event, MessageProcessor parent, List<MessageProcessor>
                processors, MessageProcessorExecutionTemplate executionTemplate)
        {
            super(event, processors, executionTemplate, true);
        }

        @Override
        protected MessageProcessor nextProcessor()
        {
            return responseProcessors.remove(0);
        }

        @Override
        public synchronized MuleEvent processNext() throws MessagingException
        {
            try
            {
                return super.processNext();
            }
            catch (MessagingException me)
            {
                completionHandler.onFailure(me);
                return null;
            }
        }
    }

}
