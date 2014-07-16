/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ObjectStore;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.UUID;
import org.mule.util.concurrent.DaemonThreadFactory;

import java.io.Serializable;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public abstract class AbstractMonitoredObjectStore<T extends Serializable> 
    implements ObjectStore<T>, Runnable, MuleContextAware, Initialisable, Disposable
{
    protected final Log logger = LogFactory.getLog(this.getClass());

    protected MuleContext context;
    protected ScheduledThreadPoolExecutor scheduler;

    /**
     * the maximum number of entries that this store keeps around. Specify <em>-1</em> if the store 
     * is supposed to be "unbounded".
     */
    protected int maxEntries = 4000;

    /**
     * The time-to-live for each message ID, specified in milliseconds, or <em>-1</em> for entries 
     * that should never expire. <b>DO NOT</b> combine this with an unbounded store!
     */
    protected int entryTTL = -1;

    /**
     * The interval for periodic bounded size enforcement and entry expiration, specified in 
     * milliseconds. Arbitrary positive values between 1 millisecond and several hours or days are 
     * possible, but should be chosen carefully according to the expected message rate to prevent 
     * out of memory conditions.
     */
    protected int expirationInterval = 1000;

    /**
     * A name for this store, can be used for logging and identification purposes.
     */
    protected String name = null;

    public void initialise() throws InitialisationException
    {
        if (name == null)
        {
            name = UUID.getUUID();
        }

        if (expirationInterval <= 0)
        {
            throw new IllegalArgumentException(CoreMessages.propertyHasInvalidValue("expirationInterval",
                    new Integer(expirationInterval)).toString());
        }

        if (scheduler == null)
        {
            this.scheduler = new ScheduledThreadPoolExecutor(1);
            scheduler.setThreadFactory(new DaemonThreadFactory(name + "-Monitor", this.getClass().getClassLoader()));
            scheduler.scheduleWithFixedDelay(this, 0, expirationInterval, TimeUnit.MILLISECONDS);
        }
    }

    public final void run()
    {
        if (context == null || context.isPrimaryPollingInstance())
        {
            expire();
        }
    }

    public void dispose()
    {
        if (scheduler != null)
        {
            scheduler.shutdown();
        }
    }

    protected MuleContext getMuleContext()
    {
        return this.context;
    }

    public void setEntryTTL(int entryTTL)
    {
        this.entryTTL = entryTTL;
    }

    public void setExpirationInterval(int expirationInterval)
    {
        this.expirationInterval = expirationInterval;
    }

    public void setMaxEntries(int maxEntries)
    {
        this.maxEntries = maxEntries;
    }

    public void setScheduler(ScheduledThreadPoolExecutor scheduler)
    {
        this.scheduler = scheduler;
    }

    public void setName(String id)
    {
        this.name = id;
    }

    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    public int getEntryTTL()
    {
        return entryTTL;
    }

    public int getExpirationInterval()
    {
        return expirationInterval;
    }

    public int getMaxEntries()
    {
        return maxEntries;
    }

    public String getName()
    {
        return name;
    }

    public ScheduledThreadPoolExecutor getScheduler()
    {
        return scheduler;
    }

    protected abstract void expire();
}
