/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import org.mule.management.stats.printers.SimplePrinter;

import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicLong;
@Deprecated
public class ServiceStatistics extends FlowConstructStatistics implements QueueStatistics
{
    private static final long serialVersionUID = -2086999226732861675L;

    private final AtomicLong sentEventSync = new AtomicLong(0);
    private final AtomicLong sentReplyToEvent = new AtomicLong(0);
    private final AtomicLong sentEventASync = new AtomicLong(0);

    // these can't sensibly converted to AtomicLong as they are processed together
    // in incQueuedEvent
    private long queuedEvent = 0;
    private long maxQueuedEvent = 0;
    private long averageQueueSize = 0;
    private long totalQueuedEvent = 0;

    private RouterStatistics inboundRouterStat = null;
    private ComponentStatistics componentStat = null;
    private RouterStatistics outboundRouterStat = null;

    public ServiceStatistics(String name)
    {
        this(name, 0);
    }

    public ServiceStatistics(String name, int threadPoolSize)
    {
        super("Service", name, threadPoolSize);
        clear();
    }

    /**
     * Enable statistics logs (this is a dynamic parameter)
     */
    @Override
    public synchronized void setEnabled(boolean b)
    {
        super.setEnabled(b);

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
        averageQueueSize = receivedEventASync.get() / totalQueuedEvent;
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
    @Deprecated
    public long getMaxExecutionTime()
    {
        return componentStat.getMaxExecutionTime();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public long getMinExecutionTime()
    {
        return componentStat.getMinExecutionTime();
    }

    /**
     * @deprecated
     */
    @Deprecated
    public long getTotalExecutionTime()
    {
        return componentStat.getTotalExecutionTime();
    }

    public synchronized long getQueuedEvents()
    {
        return queuedEvent;
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

    public long getExecutedEvents()
    {
        return componentStat.getExecutedEvents();
    }

    public void logSummary()
    {
        logSummary(new SimplePrinter(System.out));
    }

    public void logSummary(PrintWriter printer)
    {
        printer.print(this);
    }

    @Override
    public synchronized void clear()
    {
        super.clear();
        queuedEvent = 0;
        maxQueuedEvent = 0;
        totalQueuedEvent = 0;
        averageQueueSize = 0;

        sentEventSync.set(0);
        sentEventASync.set(0);
        sentReplyToEvent.set(0);

        if (getComponentStat() != null)
        {
            getComponentStat().clear();
        }
        if (getInboundRouterStat() != null)
        {
            getInboundRouterStat().clear();
        }
        if (getOutboundRouterStat() != null)
        {
            getOutboundRouterStat().clear();
        }
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
}
