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
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ObjectStore;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.UUID;
import org.mule.util.concurrent.DaemonThreadFactory;

import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public abstract class AbstractMonitoredObjectStore implements ObjectStore, Runnable, MuleContextAware, Initialisable, Disposable
{
    protected final Log logger = LogFactory.getLog(this.getClass());

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
