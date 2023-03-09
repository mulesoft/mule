/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import org.mule.api.management.stats.Statistics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
    protected final AtomicLong dispatchedMessages = new AtomicLong(0);

    private transient final List<DefaultResetOnQueryCounter> eventsReceivedCounters = new CopyOnWriteArrayList<>();
    private transient final List<DefaultResetOnQueryCounter> dispatchedMessagesCounters = new CopyOnWriteArrayList<>();

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
    @Override
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
        dispatchedMessages.set(0);
        samplePeriod = System.currentTimeMillis();
    }


    public void incReceivedEventSync()
    {
        receivedEventSync.addAndGet(1);
        for (DefaultResetOnQueryCounter eventsReceivedCounter : eventsReceivedCounters)
        {
            eventsReceivedCounter.increment();
        }
    }

    public void incReceivedEventASync()
    {
        receivedEventASync.addAndGet(1);
        for (DefaultResetOnQueryCounter eventsReceivedCounter : eventsReceivedCounters)
        {
            eventsReceivedCounter.increment();
        }
    }

    /**
     * Indicates that a new message was dispatched from a message source
     */
    public void incMessagesDispatched()
    {
        dispatchedMessages.addAndGet(1);
        for (DefaultResetOnQueryCounter dispatchedMessagesCounter : dispatchedMessagesCounters)
        {
            dispatchedMessagesCounter.increment();
        }
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

    /**
     * @return the number of messages dispatched from a source of a flow at a given time
     * 
     * @since 4.5
     */
    public long getTotalDispatchedMessages()
    {
        return dispatchedMessages.get();
    }

    public String getFlowConstructType()
    {
        return flowConstructType;
    }

    public long getSamplePeriod()
    {
        return System.currentTimeMillis() - samplePeriod;
    }


    /**
     * Provides a counter for {@link #getTotalEventsReceived() total events received} that is not affected by calls to
     * {@link #clear()} or {@link ResetOnQueryCounter#getAndReset()} calls to other instances returned by this method.
     * <p>
     * Counter initial value is set to the value of {@link #getTotalEventsReceived()} when this method is called.
     * <p>
     * If this is called concurrently with {@link #incReceivedEvents()}, there is chance of a race condition occurring where an
     * event may be counted twice. To avoid this possibility, get the counters before statistics begin to be populated.
     * 
     * @return a counter for {@link #getTotalEventsReceived()}.
     * 
     * @since 4.5
     */
    public ResetOnQueryCounter getEventsReceivedCounter()
    {
        DefaultResetOnQueryCounter counter = new DefaultResetOnQueryCounter();
        eventsReceivedCounters.add(counter);
        counter.add(getTotalEventsReceived());
        return counter;
    }

    /**
     * Provides a counter for {@link #getTotalDispatchedMessages() total dispatched messages} that is not affected by calls to
     * {@link #clear()} or {@link ResetOnQueryCounter#getAndReset()} calls to other instances returned by this method.
     * <p>
     * Counter initial value is set to the value of {@link #getTotalDispatchedMessages()} when this method is called.
     * <p>
     * If this is called concurrently with {@link #incReceivedEvents()}, there is chance of a race condition occurring where an
     * event may be counted twice. To avoid this possibility, get the counters before statistics begin to be populated.
     * 
     * @return a counter for {@link #getTotalDispatchedMessages()}.
     * 
     * @since 4.5
     */
    public ResetOnQueryCounter getDispatchedMessagesCounter()
    {
        DefaultResetOnQueryCounter counter = new DefaultResetOnQueryCounter();
        dispatchedMessagesCounters.add(counter);
        counter.add(getTotalDispatchedMessages());
        return counter;
    }
}
