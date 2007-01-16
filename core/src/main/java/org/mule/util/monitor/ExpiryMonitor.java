/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.monitor;

import org.mule.umo.lifecycle.Disposable;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ExpiryMonitor</code> can monitor objects beased on an expiry time and can
 * invoke a callback method once the object time has expired. If the object does
 * expire it is removed from this monitor.
 */

// TODO we should probably rewrite this with ScheduledExecutor for stability IF we
// need it; right now this class is unused

public class ExpiryMonitor extends TimerTask implements Disposable
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(ExpiryMonitor.class);

    private Timer timer;
    private Map monitors;

    public ExpiryMonitor()
    {
        this(1000);
    }

    public ExpiryMonitor(long monitorFrequency)
    {

        timer = new Timer(true);
        timer.schedule(this, monitorFrequency, monitorFrequency);
        monitors = new ConcurrentHashMap();
    }

    /**
     * Adds an expirable object to monitor. If the Object is already being monitored
     * it will be reset and the millisecond timeout will be ignored
     * 
     * @param milliseconds
     * @param expirable
     */
    public void addExpirable(long milliseconds, Expirable expirable)
    {
        if (isRegistered(expirable))
        {
            resetExpirable(expirable);
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Adding new expirable: " + expirable);
            }
            monitors.put(expirable, new ExpirableHolder(milliseconds, expirable));
        }
    }

    public boolean isRegistered(Expirable expirable)
    {
        return (monitors.get(expirable) != null);
    }

    public void removeExpirable(Expirable expirable)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Removing expirable: " + expirable);
        }
        monitors.remove(expirable);
    }

    public void resetExpirable(Expirable expirable)
    {
        ExpirableHolder eh = (ExpirableHolder)monitors.get(expirable);
        if (eh != null)
        {
            eh.reset();
            if (logger.isDebugEnabled())
            {
                logger.debug("Reset expirable: " + expirable);
            }
        }
    }

    /**
     * The action to be performed by this timer task.
     */
    public void run()
    {
        ExpirableHolder holder;
        for (Iterator iterator = monitors.values().iterator(); iterator.hasNext();)
        {
            holder = (ExpirableHolder)iterator.next();
            if (holder.isExpired())
            {
                removeExpirable(holder.getExpirable());
                holder.getExpirable().expired();
            }
        }
    }

    public void dispose()
    {
        logger.info("disposing monitor");
        timer.cancel();
        ExpirableHolder holder;
        for (Iterator iterator = monitors.values().iterator(); iterator.hasNext();)
        {
            holder = (ExpirableHolder)iterator.next();
            removeExpirable(holder.getExpirable());
            try
            {
                holder.getExpirable().expired();
            }
            catch (Exception e)
            {
                logger.debug(e.getMessage());
            }
        }
    }

    private class ExpirableHolder
    {

        private long milliseconds;
        private Expirable expirable;
        private long created;

        public ExpirableHolder(long milliseconds, Expirable expirable)
        {
            this.milliseconds = milliseconds;
            this.expirable = expirable;
            created = System.currentTimeMillis();
        }

        public long getMilliseconds()
        {
            return milliseconds;
        }

        public Expirable getExpirable()
        {
            return expirable;
        }

        public boolean isExpired()
        {
            return (System.currentTimeMillis() - milliseconds) > created;
        }

        public void reset()
        {
            created = System.currentTimeMillis();
        }
    }
}
