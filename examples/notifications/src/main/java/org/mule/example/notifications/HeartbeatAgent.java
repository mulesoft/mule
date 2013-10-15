/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
