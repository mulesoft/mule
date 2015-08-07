/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.VoidMuleEvent;
import org.mule.api.CompletionHandler;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.processor.AbstractNonBlockingMessageProcessor;
import org.mule.util.ObjectUtils;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.Latch;

import java.util.concurrent.Executors;

public class SensingNullMessageProcessor extends AbstractNonBlockingMessageProcessor implements MessageProcessor
{

    public MuleEvent event;
    public Latch latch = new Latch();
    public Thread thread;

    protected InternalMessageSource source = new InternalMessageSource();
    private long waitTime = 0;
    private boolean enableNonBlocking = true;
    private String appendString;

    public SensingNullMessageProcessor()
    {
        super();
    }

    public SensingNullMessageProcessor(String appendString)
    {
        this.appendString = appendString;
    }

    @Override
    protected void processNonBlocking(final MuleEvent event, CompletionHandler completionHandler) throws MuleException
    {
        sense(event);
        Executors.newSingleThreadExecutor().execute(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    MuleEvent eventToProcess = event;
                    if (StringUtils.isNotEmpty(appendString))
                    {
                        eventToProcess = append(eventToProcess);
                    }
                    event.getReplyToHandler().processReplyTo(eventToProcess, null, null);
                    latch.countDown();
                }
                catch (MuleException e)
                {
                    event.getReplyToHandler().processExceptionReplyTo(new MessagingException(event, e), null);
                }
            }
        });
    }

    private void sense(MuleEvent event)
    {
        sleepIfNeeded();
        this.event = event;
        thread = Thread.currentThread();
    }

    @Override
    protected MuleEvent processBlocking(MuleEvent event) throws MuleException
    {
        sense(event);
        if (StringUtils.isNotEmpty(appendString))
        {
            event = append(event);
        }
        latch.countDown();
        if (source.listener != null)
        {
            return source.listener.process(event);
        }
        else
        {
            if (event.getExchangePattern().hasResponse())
            {
                return event;
            }
            else
            {
                return VoidMuleEvent.getInstance();
            }
        }
    }

    @Override
    public boolean isNonBlocking(MuleEvent event)
    {
        return super.isNonBlocking(event) && enableNonBlocking;
    }

    private MuleEvent append(MuleEvent event)
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(event.getMessage().getPayload()
                                                           + appendString, event.getMuleContext()),
                                    event);
    }

    private void sleepIfNeeded()
    {
        if (waitTime > 0)
        {
            try
            {
                Thread.sleep(waitTime);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void clear()
    {
        event = null;
    }

    public MessageSource getMessageSource()
    {
        return source;
    }

    public void setWaitTime(long waitTime)
    {
        this.waitTime = waitTime;
    }

    class InternalMessageSource implements MessageSource
    {
        MessageProcessor listener;

        public void setListener(MessageProcessor listener)
        {
            this.listener = listener;

        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }
    }
}
