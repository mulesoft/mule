/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.store;

import org.mule.api.MuleContext;
import org.mule.api.store.ObjectStore;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Disposable;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.concurrent.DaemonThreadFactory;
import org.mule.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * TODO
 */
public abstract class AbstractMonitoredObjectStore implements ObjectStore, Runnable, MuleContextAware, Initialisable, Disposable
{
    protected final Log logger = LogFactory.getLog(this.getClass());

    /**
     * Default constructor for IdempotentInMemoryMessageIdStore.
     *
     * @param name               a name for this store, can be used for logging and identification
     * purposes
     * @param maxEntries         the maximum number of entries that this store keeps around.
     * Specify <em>-1</em> if the store is supposed to be "unbounded".
     * @param entryTTL           the time-to-live for each message ID, specified in seconds, or
     * <em>-1</em> for entries that should never expire. <b>DO NOT</b>
     * combine this with an unbounded store!
     * @param expirationInterval the interval for periodic bounded size enforcement and
     * entry expiration, specified in seconds. Arbitrary positive values
     * between 1 second and several hours or days are possible, but should be
     * chosen carefully according to the expected message rate to prevent
     * OutOfMemory conditions.
     */
    protected ScheduledThreadPoolExecutor scheduler;
    protected int maxEntries = 4000;
    protected int entryTTL = -1;
    protected int expirationInterval = 1000;
    protected String name;
    protected MuleContext context;


    public void initialise() throws InitialisationException
    {
        if (name == null)
        {
            name = UUID.getUUID();
            //throw new IllegalArgumentException(CoreMessages.propertyHasInvalidValue("storeId", "null").getMessage());
        }

        if (expirationInterval <= 0)
        {
            throw new IllegalArgumentException(CoreMessages.propertyHasInvalidValue("expirationInterval",
                    new Integer(expirationInterval)).toString());
        }

        if (scheduler == null)
        {
            this.scheduler = new ScheduledThreadPoolExecutor(1);
            scheduler.setThreadFactory(new DaemonThreadFactory(name + "-Monitor"));
            scheduler.scheduleWithFixedDelay(this, 0, expirationInterval, TimeUnit.MILLISECONDS);
        }
    }

    public final void run()
    {
        expire();
    }

    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown it should just be logged and processing should continue.
     * This method should not throw Runtime exceptions.
     */
    public void dispose()
    {
        if(scheduler!=null)
        {
            scheduler.shutdown();
        }
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
