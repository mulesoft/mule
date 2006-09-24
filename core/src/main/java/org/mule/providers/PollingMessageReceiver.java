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

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

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
        try
        {
            getWorkManager().scheduleWork(this, WorkManager.INDEFINITE, null, connector);
        }
        catch (WorkException e)
        {
            stopped.set(true);
            throw new InitialisationException(new Message(Messages.FAILED_TO_SCHEDULE_WORK), e, this);
        }
    }

    public void run()
    {
        try
        {
            Thread.sleep(STARTUP_DELAY);
            while (!stopped.get())
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
                Thread.sleep(frequency);
            }
        }
        catch (InterruptedException e)
        {
            // Exit thread
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
