/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.stats;

import org.mule.api.management.stats.Statistics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics common to flows and services
 */
public abstract class AbstractFlowConstructStatistics implements Statistics
{
    private static final long serialVersionUID = 5337576392583767442L;

    protected final String flowConstructType;
    protected String name;
    protected boolean enabled = false;
    private long samplePeriod = 0;
    protected final AtomicLong receivedEventSync = new AtomicLong(0);
    protected final AtomicLong receivedEventASync = new AtomicLong(0);

    public AbstractFlowConstructStatistics(String flowConstructType, String name)
    {
        this.name = name;
        this.flowConstructType = flowConstructType;
    }

    /**
     * Enable statistics logs (this is a dynamic parameter)
     */
    public synchronized void setEnabled(boolean b)
    {
        enabled = b;
    }

    /**
     * Are statistics logged
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    public synchronized String getName()
    {
        return name;
    }

    public synchronized void setName(String name)
    {
        this.name = name;
    }

    public synchronized void clear()
    {
        receivedEventSync.set(0);
        receivedEventASync.set(0);
        samplePeriod = System.currentTimeMillis();
    }


    public void incReceivedEventSync()
    {
        receivedEventSync.addAndGet(1);
    }

    public void incReceivedEventASync()
    {
        receivedEventASync.addAndGet(1);
    }

    public long getAsyncEventsReceived()
    {
        return receivedEventASync.get();
    }

    public long getSyncEventsReceived()
    {
        return receivedEventSync.get();
    }

    public long getTotalEventsReceived()
    {
        return getSyncEventsReceived() + getAsyncEventsReceived();
    }

    public String getFlowConstructType()
    {
        return flowConstructType;
    }

    public long getSamplePeriod()
    {
        return System.currentTimeMillis() - samplePeriod;
    }
}
