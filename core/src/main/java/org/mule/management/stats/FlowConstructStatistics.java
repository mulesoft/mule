/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.stats;

import org.mule.api.processor.ProcessingStrategy;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;

import java.util.concurrent.atomic.AtomicLong;

public class FlowConstructStatistics extends AbstractFlowConstructStatistics implements QueueStatistics
{
    private static final long serialVersionUID = 5337576392583767442L;
    private final AtomicLong executionError = new AtomicLong(0);
    private final AtomicLong fatalError = new AtomicLong(0);
    private int threadPoolSize = 0;
    protected final ComponentStatistics flowStatistics = new ComponentStatistics();
    
    // these can't sensibly converted to AtomicLong as they are processed together
    // in incQueuedEvent
    private long queuedEvent = 0;
    private long maxQueuedEvent = 0;
    private long averageQueueSize = 0;
    private long totalQueuedEvent = 0;


    public FlowConstructStatistics(String flowConstructType, String name, ProcessingStrategy processingStrategy)
    {
        super(flowConstructType, name);
        flowStatistics.setEnabled(enabled);
        if (processingStrategy instanceof AsynchronousProcessingStrategy)
        {
            this.threadPoolSize = ((AsynchronousProcessingStrategy) processingStrategy).getMaxThreads();
        }
        if (this.getClass() == FlowConstructStatistics.class)
        {
            clear();
        }
    }
    
    public FlowConstructStatistics(String flowConstructType, String name, int maxThreadSize)
    {
        super(flowConstructType, name);
        flowStatistics.setEnabled(enabled);
        this.threadPoolSize = maxThreadSize;
        if (this.getClass() == FlowConstructStatistics.class)
        {
            clear();
        }
    }

    public FlowConstructStatistics(String flowConstructType, String name)
    {
        this(flowConstructType, name, null);
    }
    
    /**
     * Are statistics logged
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    public void incExecutionError()
    {
        executionError.addAndGet(1);
    }

    public void incFatalError()
    {
        fatalError.addAndGet(1);
    }

    /**
     * Enable statistics logs (this is a dynamic parameter)
     */
    public synchronized void setEnabled(boolean b)
    {
        super.setEnabled(b);
        flowStatistics.setEnabled(enabled);
    }

    public synchronized void clear()
    {
        super.clear();

        executionError.set(0);
        fatalError.set(0);        
        if (flowStatistics != null)
        {
            flowStatistics.clear();
        }
    }

    public void addCompleteFlowExecutionTime(long time)
    {
        flowStatistics.addCompleteExecutionTime(time);
    }

    public void addFlowExecutionBranchTime(long time, long total)
    {
        flowStatistics.addExecutionBranchTime(time == total, time, total);
    }

    public long getAverageProcessingTime()
    {
        return flowStatistics.getAverageExecutionTime();
    }

    public long getProcessedEvents()
    {
        return flowStatistics.getExecutedEvents();
    }

    public long getMaxProcessingTime()
    {
        return flowStatistics.getMaxExecutionTime();
    }

    public long getMinProcessingTime()
    {
        return flowStatistics.getMinExecutionTime();
    }

    public long getTotalProcessingTime()
    {
        return flowStatistics.getTotalExecutionTime();
    }

    public long getExecutionErrors()
    {
        return executionError.get();
    }

    public long getFatalErrors()
    {
        return fatalError.get();
    }

    public int getThreadPoolSize()
    {
        return threadPoolSize;
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
    
    public synchronized long getAverageQueueSize()
    {
        return averageQueueSize;
    }

}