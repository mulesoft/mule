/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.monitor;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.Disposable;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.concurrent.DaemonThreadFactory;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ExpiryMonitor</code> can monitor objects beased on an expiry time and can
 * invoke a callback method once the object time has expired. If the object does
 * expire it is removed from this monitor.
 */
public class ExpiryMonitor implements Runnable, Disposable
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(ExpiryMonitor.class);

    protected ScheduledThreadPoolExecutor scheduler;

    private Map monitors;

    private int monitorFrequency;

    private String name;
    
    private ClassLoader contextClassLoader;

    private MuleContext muleContext;

    private boolean onPollingNodeOnly;

    public ExpiryMonitor(MuleContext muleContext, boolean onPollingNodeOnly)
    {
        this.muleContext = muleContext;
        this.onPollingNodeOnly = onPollingNodeOnly;
    }

    public ExpiryMonitor(String name, MuleContext muleContext, boolean onPollingNodeOnly)
    {
        this(name, 1000, muleContext, onPollingNodeOnly);
    }

    public ExpiryMonitor(String name, int monitorFrequency, MuleContext muleContext, boolean onPollingNodeOnly)
    {
        this(muleContext, onPollingNodeOnly);
        this.name = name;
        this.monitorFrequency = monitorFrequency;
        init();
    }

    public ExpiryMonitor(String name, int monitorFrequency, ClassLoader contextClassLoader, MuleContext muleContext, boolean onPollingNodeOnly)
    {
        this(muleContext, onPollingNodeOnly);
        this.name = name;
        this.monitorFrequency = monitorFrequency;
        this.contextClassLoader = contextClassLoader;
        init();
    }
    
    public ExpiryMonitor(String name, int monitorFrequency, ScheduledThreadPoolExecutor scheduler, MuleContext muleContext, boolean onPollingNodeOnly)
    {
        this(muleContext, onPollingNodeOnly);
        this.name = name;
        this.monitorFrequency = monitorFrequency;
        this.scheduler = scheduler;
        init();
    }

    protected void init()
    {
        if (monitorFrequency <= 0)
        {
            throw new IllegalArgumentException(CoreMessages.propertyHasInvalidValue("monitorFrequency",
                    new Integer(monitorFrequency)).toString());
        }
        monitors = new ConcurrentHashMap();
        if (scheduler == null)
        {
            this.scheduler = new ScheduledThreadPoolExecutor(1);
            scheduler.setThreadFactory(new DaemonThreadFactory(name + ".expiry.monitor", contextClassLoader));
            scheduler.scheduleWithFixedDelay(this, 0, monitorFrequency,
                                             TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Adds an expirable object to monitor. If the Object is already being monitored
     * it will be reset and the millisecond timeout will be ignored
     *
     * @param value     the expiry value
     * @param timeUnit  The time unit of the Expiry value
     * @param expirable the objec that will expire
     */
    public void addExpirable(long value, TimeUnit timeUnit, Expirable expirable)
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
            monitors.put(expirable, new ExpirableHolder(timeUnit.toMillis(value), expirable));
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
        ExpirableHolder eh = (ExpirableHolder) monitors.get(expirable);
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

        if (!onPollingNodeOnly || muleContext == null || muleContext.isPrimaryPollingInstance())
        {
            for (Iterator iterator = monitors.values().iterator(); iterator.hasNext();)
            {
                holder = (ExpirableHolder) iterator.next();
                if (holder.isExpired())
                {
                    removeExpirable(holder.getExpirable());
                    holder.getExpirable().expired();
                }
            }
        }
    }

    public void dispose()
    {
        logger.info("disposing monitor");
        scheduler.shutdown();
        ExpirableHolder holder;
        for (Iterator iterator = monitors.values().iterator(); iterator.hasNext();)
        {
            holder = (ExpirableHolder) iterator.next();
            removeExpirable(holder.getExpirable());
            try
            {
                holder.getExpirable().expired();
            }
            catch (Exception e)
            {
                // TODO MULE-863: What should we really do?
                logger.debug(e.getMessage());
            }
        }
    }

    private static class ExpirableHolder
    {

        private Expirable expirable;
        private long milliseconds;
        private long created;

        public ExpirableHolder(long milliseconds, Expirable expirable)
        {
            this.milliseconds = milliseconds;
            this.expirable = expirable;
            created = System.currentTimeMillis();
        }

        public long getMilliSeconds()
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
