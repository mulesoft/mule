/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.notifications;

import org.mule.AbstractAgent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

/**
 * A simple agent that fire {@link org.mule.example.notifications.HeartbeatNotification} events at a given frequency to
 * notify that the server is alive and well.
 */
public class HeartbeatAgent extends AbstractAgent
{
    public static final String NAME = "Heartbeat";

    private long frequency = 10000;

    public HeartbeatAgent()
    {
        super(NAME);
    }

    public long getFrequency()
    {
        return frequency;
    }

    public void setFrequency(long frequency)
    {
        this.frequency = frequency;
    }

    public void initialise() throws InitialisationException
    {
        //No Op
    }

    public void start() throws MuleException
    {
        try
        {
            muleContext.getWorkManager().scheduleWork(new Heartbeat());
        }
        catch (WorkException e)
        {
            throw new DefaultMuleException(e);
        }
    }

    public void stop() throws MuleException
    {
        //No Op
    }

    public void dispose()
    {
        //No Op
    }

    public class Heartbeat implements Work
    {
        public void release()
        {
            //No Op
        }

        @SuppressWarnings("synthetic-access")
        public void run()
        {
            while(true)
            {
                muleContext.fireNotification(new HeartbeatNotification(muleContext));
                try
                {
                    Thread.sleep(frequency);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}
