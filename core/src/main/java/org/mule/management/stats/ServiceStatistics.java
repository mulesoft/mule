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

public class ServiceStatistics implements Statistics
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2086999226732861674L;

    private String name;
    private long receivedEventSync = 0;
    private long receivedEventASync = 0;
    private long queuedEvent = 0;
    private long maxQueuedEvent = 0;
    private long averageQueueSize = 0;
    private long totalQueuedEvent = 0;
    private long sentEventSync = 0;
    private long sentReplyToEvent = 0;
    private long sentEventASync = 0;
    private long executionError = 0;
    private long fatalError = 0;

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

    /**
     * The constructor
     *
     * @param name
     */
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

    public synchronized void incReceivedEventSync()
    {
        receivedEventSync++;
    }

    public synchronized void incReceivedEventASync()
    {
        receivedEventASync++;
    }

    public synchronized void incExecutionError()
    {
        executionError++;
    }

    public synchronized void incFatalError()
    {
        fatalError++;
    }

    public synchronized void incSentEventSync()
    {
        sentEventSync++;
    }

    public synchronized void incSentEventASync()
    {
        sentEventASync++;
    }

    public synchronized void incSentReplyToEvent()
    {
        sentReplyToEvent++;
    }

    public synchronized void incQueuedEvent()
    {
        queuedEvent++;
        totalQueuedEvent++;
        if (queuedEvent > maxQueuedEvent)
        {
            maxQueuedEvent = queuedEvent;
        }
        // if(queuedEvent > 1) {
        averageQueueSize = Math.round(getAsyncEventsReceived() / totalQueuedEvent);
        // }
    }

    public synchronized void decQueuedEvent()
    {
        queuedEvent--;
    }

    public long getAverageExecutionTime()
    {
        return componentStat.getAverageExecutionTime();
    }

    public long getAverageQueueSize()
    {
        return averageQueueSize;
    }

    public long getMaxQueueSize()
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
        return fatalError;
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

    public long getQueuedEvents()
    {
        return queuedEvent;
    }

    public long getAsyncEventsReceived()
    {
        return receivedEventASync;
    }

    public long getSyncEventsReceived()
    {
        return receivedEventSync;
    }

    public long getReplyToEventsSent()
    {
        return sentReplyToEvent;
    }

    public long getSyncEventsSent()
    {
        return sentEventSync;
    }

    public long getAsyncEventsSent()
    {
        return sentEventASync;
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
        return executionError;
    }

    public synchronized String getName()
    {
        return name;
    }

    public synchronized void setName(String name)
    {
        this.name = name;
    }

    /**
     * log in info level the main statistics
     */
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
        receivedEventSync = 0;
        receivedEventASync = 0;
        queuedEvent = 0;
        maxQueuedEvent = 0;
        totalQueuedEvent = 0;
        averageQueueSize = 0;

        sentEventSync = 0;
        sentEventASync = 0;
        sentReplyToEvent = 0;

        executionError = 0;
        fatalError = 0;

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
