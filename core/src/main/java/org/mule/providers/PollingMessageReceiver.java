/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import javax.resource.spi.work.Work;

/**
 * <p>
 * <code>PollingMessageReceiver</code> implements a polling message receiver. The
 * receiver provides a poll method that implementations should implement to execute
 * their custom code. Note that the receiver will not poll if the associated
 * connector is not started.
 */
public abstract class PollingMessageReceiver extends AbstractMessageReceiver implements Work
{
    public static final long DEFAULT_POLL_FREQUENCY = 1000;
    public static final long STARTUP_DELAY = 1000;

    protected long frequency = DEFAULT_POLL_FREQUENCY;

    public PollingMessageReceiver(UMOConnector connector,
                                  UMOComponent component,
                                  final UMOEndpoint endpoint,
                                  Long frequency) throws InitialisationException
    {
        super(connector, component, endpoint);
        this.frequency = frequency.longValue();
    }

    public void doStart() throws UMOException
    {
//        // this is the old way of polling, constantly occupying a thread for no good reason
//        try
//        {
//            getWorkManager().scheduleWork(this, WorkManager.INDEFINITE, null, connector);
//        }
//        catch (WorkException e)
//        {
//            stopped.set(true);
//            throw new InitialisationException(new Message(Messages.FAILED_TO_SCHEDULE_WORK), e, this);
//        }

        // TODO: handle exceptions & keep the returned ScheduledFuture for cancelling ourselves

        // we use scheduleWithFixedDelay to prevent queue-up of tasks when polling
        // takes longer than the specified frequency, e.g. when the polled database
        // or network is slow or returns large amounts of data.
        connector.getScheduler().scheduleWithFixedDelay(this, STARTUP_DELAY, frequency, TimeUnit.MILLISECONDS);
    }

//    public void run()
//    {
//        try
//        {
//            Thread.sleep(STARTUP_DELAY);
//            while (!stopped.get())
//            {
//                connected.whenTrue(null);
//                try
//                {
//                    poll();
//                }
//                catch (InterruptedException e)
//                {
//                    return;
//                }
//                catch (Exception e)
//                {
//                    handleException(e);
//                }
//                Thread.sleep(frequency);
//            }
//        }
//        catch (InterruptedException e)
//        {
//            // Exit thread
//        }
//    }

    // the new run can safely exit after each poll() since it will be
    // invoked again by the connector's scheduler
    public void run()
    {
        try
        {
            if (!stopped.get())
            {
                connected.whenTrue(null);
                try
                {
                    poll();
                }
                catch (InterruptedException e)
                {
                    return;
                }
                catch (Exception e)
                {
                    handleException(e);
                }
            }
        }
        catch (InterruptedException e)
        {
            // ignore? re-raise interrupted state?
        }
    }

    public void release()
    {
        this.stop();
    }

    public void setFrequency(long l)
    {
        if (l <= 0)
        {
            frequency = DEFAULT_POLL_FREQUENCY;
        }
        else
        {
            frequency = l;
        }
    }

    public long getFrequency()
    {
        return frequency;
    }

    public abstract void poll() throws Exception;

}
