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
 */
package org.mule.util.monitor;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <code>ExpiryMonitor</code> can monitor objects beased on an expiry time and
 * can invoke a callback method once the object time has expired.  If the object
 * does expire it is removed from this monitor
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class ExpiryMonitor extends TimerTask
{
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


    public void addExpirable(long milliseconds, Expirable expirable)
    {
        monitors.put(expirable, new ExpriableHolder(milliseconds, expirable));
    }

    public void removeExpirable(Expirable expirable)
    {
        monitors.remove(expirable);
    }

    public void resetExpirable(Expirable expirable)
    {
        ExpriableHolder eh = (ExpriableHolder)monitors.get(expirable);
        if(eh!=null) eh.reset();
    }

    /**
     * The action to be performed by this timer task.
     */
    public void run()
    {
        ExpriableHolder holder;
        for (Iterator iterator = monitors.values().iterator(); iterator.hasNext();)
        {
            holder = (ExpriableHolder)iterator.next();
            if(holder.isExpired()) {
                removeExpirable(holder.getExpirable());
                holder.getExpirable().expired();
            }
        }
    }

    private class ExpriableHolder {

        private long milliseconds;
        private Expirable expirable;
        private long created;

        public ExpriableHolder(long milliseconds, Expirable expirable)
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
            return (created + milliseconds) > System.currentTimeMillis();
        }

        public void reset() {
            created = System.currentTimeMillis();
        }
    }
}
