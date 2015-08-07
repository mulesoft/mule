/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.interceptor;

import org.mule.DefaultMuleEvent;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.interceptor.Interceptor;
import org.mule.api.transport.ReplyToHandler;
import org.mule.management.stats.ProcessingTime;
import org.mule.processor.AbstractRequestResponseMessageProcessor;

/**
 * <code>EnvelopeInterceptor</code> is an intercepter that will fire before and after
 * an event is received.
 */
public abstract class AbstractEnvelopeInterceptor extends AbstractRequestResponseMessageProcessor
        implements Interceptor, FlowConstructAware
{

    protected FlowConstruct flowConstruct;

    /**
     * This method is invoked before the event is processed
     */
    public abstract MuleEvent before(MuleEvent event) throws MuleException;

    /**
     * This method is invoked after the event has been processed, unless an exception was thrown
     */
    public abstract MuleEvent after(MuleEvent event) throws MuleException;

    /**
     *  This method is always invoked after the event has been processed,
     */
    public abstract MuleEvent last(MuleEvent event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException;

    @Override
    protected MuleEvent processBlocking(MuleEvent event) throws MuleException
    {
        long startTime = System.currentTimeMillis();
        ProcessingTime time = event.getProcessingTime();
        boolean exceptionWasThrown = true;
        MuleEvent resultEvent = event;
        try
        {
            resultEvent = after(processNext(before(resultEvent)));
            exceptionWasThrown = false;
        }
        finally
        {
            resultEvent = last(resultEvent, time, startTime, exceptionWasThrown);
        }
        return resultEvent;
    }

    @Override
    protected MuleEvent processNonBlocking(final MuleEvent event) throws MuleException
    {
        final long startTime = System.currentTimeMillis();
        final ProcessingTime time = event.getProcessingTime();
        MuleEvent responseEvent = event;

        final ReplyToHandler originalReplyToHandler = event.getReplyToHandler();
        responseEvent = new DefaultMuleEvent(event, new ResponseReplyToHandler(originalReplyToHandler, time, startTime));
        // Update RequestContext ThreadLocal for backwards compatibility
        OptimizedRequestContext.unsafeSetEvent(responseEvent);

        try
        {
            responseEvent = processNext(processRequest(responseEvent));
            if (!(responseEvent instanceof NonBlockingVoidMuleEvent))
            {
                responseEvent = processResponse(responseEvent);
            }
        }
        catch (Exception exception)
        {
            last(responseEvent, time, startTime, true);
            throw exception;
        }
        return responseEvent;
    }

    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }

    class ResponseReplyToHandler implements ReplyToHandler
    {

        private final ReplyToHandler originalReplyToHandler;
        private final ProcessingTime time;
        private final long startTime;

        public ResponseReplyToHandler(ReplyToHandler originalReplyToHandler, ProcessingTime time, long startTime)
        {
            this.originalReplyToHandler = originalReplyToHandler;
            this.time = time;
            this.startTime = startTime;
        }

        @Override
        public void processReplyTo(final MuleEvent event, MuleMessage returnMessage, Object replyTo) throws
                                                                                                     MuleException
        {
            MuleEvent response = event;
            boolean exceptionWasThrown = true;
            try
            {
                response = after(event);
                originalReplyToHandler.processReplyTo(response, null, replyTo);
                exceptionWasThrown = false;
            }
            finally
            {
                last(response, time, startTime, false);
            }
        }

        @Override
        public void processExceptionReplyTo(MessagingException exception, Object replyTo)
        {
            try
            {
                originalReplyToHandler.processExceptionReplyTo(exception, replyTo);
            }
            finally
            {
                try
                {
                    last(exception.getEvent(), time, startTime, true);
                }
                catch (MuleException muleException)
                {
                    throw new MuleRuntimeException(muleException);
                }
            }
        }
    }
}
