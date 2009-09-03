/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.stats;

import org.mule.api.management.stats.Statistics;
import org.mule.management.stats.printers.SimplePrinter;

import java.io.PrintWriter;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

public class ServiceStatistics implements Statistics
{
    private static final long serialVersionUID = -2086999226732861675L;

    private String name;
    private final AtomicLong receivedEventSync = new AtomicLong(0);
    private final AtomicLong receivedEventASync = new AtomicLong(0);
    private final AtomicLong sentEventSync = new AtomicLong(0);
    private final AtomicLong sentReplyToEvent = new AtomicLong(0);
    private final AtomicLong sentEventASync = new AtomicLong(0);
    private final AtomicLong executionError = new AtomicLong(0);
    private final AtomicLong fatalError = new AtomicLong(0);

    // these can't sensibly converted to AtomicLong as they are processed together
    // in incQueuedEvent
    private long queuedEvent = 0;
    private long maxQueuedEvent = 0;
    private long averageQueueSize = 0;
    private long totalQueuedEvent = 0;

    private int threadPoolSize = 0;
    private long samplePeriod = 0;
    private boolean enabled = false;
    
    private RouterStatistics inboundRouterStat = null;
    private ComponentStatistics componentStat = null;
    private RouterStatistics outboundRouterStat = null;

    public ServiceStatistics(String name)
    {
        this(name, 0);
    }

    public ServiceStatistics(String name, int threadPoolSize)
    {
        super();
        this.name = name;

        this.threadPoolSize = threadPoolSize;
        clear();
    }

    /**
     * Are statistics logged
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Enable statistics logs (this is a dynamic parameter)
     */
    public synchronized void setEnabled(boolean b)
    {
        enabled = b;

        if (inboundRouterStat != null)
        {
            inboundRouterStat.setEnabled(b);
        }
        if (componentStat != null)
        {
            componentStat.setEnabled(b);
        }
        if (outboundRouterStat != null)
        {
            outboundRouterStat.setEnabled(b);
        }
    }

    public void incReceivedEventSync()
    {
        receivedEventSync.addAndGet(1);
    }

    public void incReceivedEventASync()
    {
        receivedEventASync.addAndGet(1);
    }

    public void incExecutionError()
    {
        executionError.addAndGet(1);
    }

    public void incFatalError()
    {
        fatalError.addAndGet(1);
    }

    public void incSentEventSync()
    {
        sentEventSync.addAndGet(1);
    }

    public void incSentEventASync()
    {
        sentEventASync.addAndGet(1);
    }

    public void incSentReplyToEvent()
    {
        sentReplyToEvent.addAndGet(1);
    }

    public synchronized void incQueuedEvent()
    {
        queuedEvent++;
        totalQueuedEvent++;
        if (queuedEvent > maxQueuedEvent)
        {
            maxQueuedEvent = queuedEvent;
        }
        averageQueueSize = Math.round(receivedEventASync.get() / totalQueuedEvent);
    }

    public synchronized void decQueuedEvent()
    {
        queuedEvent--;
    }

    public long getAverageExecutionTime()
    {
        return componentStat.getAverageExecutionTime();
    }

    public synchronized long getAverageQueueSize()
    {
        return averageQueueSize;
    }

    public synchronized long getMaxQueueSize()
    {
        return maxQueuedEvent;
    }

    /**
     * @deprecated
     */
    public long getMaxExecutionTime()
    {
        return componentStat.getMaxExecutionTime();
    }

    public long getFatalErrors()
    {
        return fatalError.get();
    }

    /**
     * @deprecated
     */
    public long getMinExecutionTime()
    {
        return componentStat.getMinExecutionTime();
    }

    /**
     * @deprecated
     */
    public long getTotalExecutionTime()
    {
        return componentStat.getTotalExecutionTime();
    }

    public synchronized long getQueuedEvents()
    {
        return queuedEvent;
    }

    public long getAsyncEventsReceived()
    {
        return receivedEventASync.get();
    }

    public long getSyncEventsReceived()
    {
        return receivedEventSync.get();
    }

    public long getReplyToEventsSent()
    {
        return sentReplyToEvent.get();
    }

    public long getSyncEventsSent()
    {
        return sentEventSync.get();
    }

    public long getAsyncEventsSent()
    {
        return sentEventASync.get();
    }

    public long getTotalEventsSent()
    {
        return getSyncEventsSent() + getAsyncEventsSent();
    }

    public long getTotalEventsReceived()
    {
        return getSyncEventsReceived() + getAsyncEventsReceived();
    }

    public long getExecutedEvents()
    {
        return componentStat.getExecutedEvents();
    }

    public long getExecutionErrors()
    {
        return executionError.get();
    }

    public synchronized String getName()
    {
        return name;
    }

    public synchronized void setName(String name)
    {
        this.name = name;
    }

    public void logSummary()
    {
        logSummary(new SimplePrinter(System.out));
    }

    public void logSummary(PrintWriter printer)
    {
        printer.print(this);
    }

    public synchronized void clear()
    {
        receivedEventSync.set(0);
        receivedEventASync.set(0);
        queuedEvent = 0;
        maxQueuedEvent = 0;
        totalQueuedEvent = 0;
        averageQueueSize = 0;

        sentEventSync.set(0);
        sentEventASync.set(0);
        sentReplyToEvent.set(0);

        executionError.set(0);
        fatalError.set(0);

        if (getInboundRouterStat() != null)
        {
            getInboundRouterStat().clear();
        }
        if (getOutboundRouterStat() != null)
        {
            getOutboundRouterStat().clear();
        }

        samplePeriod = System.currentTimeMillis();
    }

    public RouterStatistics getInboundRouterStat()
    {
        return inboundRouterStat;
    }

    public void setInboundRouterStat(RouterStatistics inboundRouterStat)
    {
        this.inboundRouterStat = inboundRouterStat;
        this.inboundRouterStat.setEnabled(enabled);
    }

    public RouterStatistics getOutboundRouterStat()
    {
        return outboundRouterStat;
    }

    public void setOutboundRouterStat(RouterStatistics outboundRouterStat)
    {
        this.outboundRouterStat = outboundRouterStat;
        this.outboundRouterStat.setEnabled(enabled);
    }
    
    public ComponentStatistics getComponentStat()
    {
        return componentStat;
    }

    public void setComponentStat(ComponentStatistics componentStat)
    {
        this.componentStat = componentStat;
        this.componentStat.setEnabled(enabled);
    }

    public int getThreadPoolSize()
    {
        return threadPoolSize;
    }

    public long getSamplePeriod()
    {
        return System.currentTimeMillis() - samplePeriod;
    }
}
