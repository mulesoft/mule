/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.providers;

import org.mule.InitialisationException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.timer.EventTimerTask;
import org.mule.util.timer.TimeEvent;
import org.mule.util.timer.TimeEventListener;

import java.util.Timer;

/**
 * <p><code>PollingMessageReceiver</code> implements a polling message receiver.
 * The receiver provides a poll method that implementations should implement to
 * execute their custom code.  Note that the receiver will not poll if the associated
 * connector is not started.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class PollingMessageReceiver extends AbstractMessageReceiver implements TimeEventListener
{
    public static final long DEFAULT_POLL_FREQUENCY = 1000;
    public static final long STARTUP_DELAY = 4000;

    private Timer timer;
    private long frequency = DEFAULT_POLL_FREQUENCY;

    public PollingMessageReceiver(UMOConnector connector,
                                  UMOComponent component,
                                  final UMOEndpoint endpoint, Long frequency) throws InitialisationException
    {
        create(connector, component, endpoint);
        this.frequency = frequency.longValue();
        timer = new Timer();

        EventTimerTask task = new EventTimerTask(this);
        timer.schedule(task, STARTUP_DELAY, this.frequency);
        task.start();
    }

    /* (non-Javadoc)
     * @see org.mule.util.timer.TimeEventListener#timeExpired(org.mule.util.timer.TimeEvent)
     */
    public void timeExpired(TimeEvent event)
    {
        if(endpoint.getConnector().isDisposed() || disposing.get()) {
            timer.cancel();
            return;
        }
        if (endpoint.getConnector().isStarted())
        {
            poll();
        }
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

    protected void doDispose() throws UMOException
    {
        if(timer!=null) timer.cancel();
        timer=null;
    }

    public abstract void poll();
}