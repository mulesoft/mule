/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck;

import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.util.ObjectUtils;
import org.mule.util.concurrent.Latch;

public class SensingNullMessageProcessor implements MessageProcessor
{
    public MuleEvent event;
    protected InternalMessageSource source = new InternalMessageSource();
    private long waitTime = 0;
    public Latch latch = new Latch();

    public MuleEvent process(MuleEvent event) throws MuleException
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
        this.event = event;
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
