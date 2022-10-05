/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import org.mule.api.processor.ProcessingStrategy;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;

import static java.lang.Boolean.getBoolean;
import static org.mule.api.config.MuleProperties.COMPUTE_CONNECTION_ERRORS_IN_STATS;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class FlowConstructStatistics extends AbstractFlowConstructStatistics implements QueueStatistics
{
    private static final long serialVersionUID = 5337576392583767442L;
    
    private boolean computeConnectionErrorsInApplicationStats = getBoolean(COMPUTE_CONNECTION_ERRORS_IN_STATS);
        
    private final AtomicLong executionError = new AtomicLong(0);
    private final AtomicLong fatalError = new AtomicLong(0);
    private final AtomicLong connectionErrors = new AtomicLong(0);
    private int threadPoolSize = 0;
    protected final ComponentStatistics flowStatistics = new ComponentStatistics();
    
    // these can't sensibly converted to AtomicLong as they are processed together
    // in incQueuedEvent
    private long queuedEvent = 0;
    private long maxQueuedEvent = 0;
    private long averageQueueSize = 0;
    private long totalQueuedEvent = 0;

    private transient final List<DefaultResetOnQueryCounter> executionErrorsCounters = new CopyOnWriteArrayList<>();
    private transient final List<DefaultResetOnQueryCounter> connectionErrorsCounters = new CopyOnWriteArrayList<>();
    private transient final List<DefaultResetOnQueryCounter> fatalErrorsCounters = new CopyOnWriteArrayList<>();

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
    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    public void incExecutionError()
    {
        executionError.addAndGet(1);
        for (DefaultResetOnQueryCounter executionErrorsCounter : executionErrorsCounters)
        {
            executionErrorsCounter.increment();
        }
    }

    public void incFatalError()
    {
        fatalError.addAndGet(1);
        for (DefaultResetOnQueryCounter fatalErrorsCounter : fatalErrorsCounters)
        {
            fatalErrorsCounter.increment();
        }
    }

    /**
     * Enable statistics logs (this is a dynamic parameter)
     */
    @Override
    public synchronized void setEnabled(boolean b)
    {
        super.setEnabled(b);
        flowStatistics.setEnabled(enabled);
    }

    @Override
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
    
    public long getConnectionErrors()
    {
        return connectionErrors.get();
    }
    
    public int getThreadPoolSize()
    {
        return threadPoolSize;
    }

    @Override
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

    @Override
    public synchronized void decQueuedEvent()
    {
        queuedEvent--;
    }
    
    public synchronized long getAverageQueueSize()
    {
        return averageQueueSize;
    }

    public void incConnectionErrors()
    {
        connectionErrors.addAndGet(1);
    }

    protected boolean computeConnectionErrors()
    {
        return computeConnectionErrorsInApplicationStats;
    }
    
    protected void setComputeConnectionErrors(boolean computeConnectionErrorsInApplicationStats) {
        this.computeConnectionErrorsInApplicationStats = computeConnectionErrorsInApplicationStats;
    }

    /**
     * Provides a counter for {@link #getExecutionErrors() execution errors} that is not affected by calls to {@link #clear()} or
     * {@link ResetOnQueryCounter#getAndReset()} calls to other instances returned by this method.
     * <p>
     * Counter initial value is set to the value of {@link #getExecutionErrors()} when this method is called.
     * <p>
     * If this is called concurrently with {@link #incExecutionError()}, there is chance of a race condition occurring where an
     * event may be counted twice. To avoid this possibility, get the counters before statistics begin to be populated.
     * 
     * @return a counter for {@link #getExecutionErrors()}.
     * 
     * @since 4.5
     */
    public ResetOnQueryCounter getExecutionErrorsCounter()
    {
        DefaultResetOnQueryCounter counter = new DefaultResetOnQueryCounter();
        executionErrorsCounters.add(counter);
        counter.add(getExecutionErrors());
        return counter;
    }

    /**
     * Provides a counter for {@link #getFatalErrors() fatal errors} that is not affected by calls to {@link #clear()} or
     * {@link ResetOnQueryCounter#getAndReset()} calls to other instances returned by this method.
     * <p>
     * Counter initial value is set to the value of {@link #getFatalErrors()} when this method is called.
     * <p>
     * If this is called concurrently with {@link #incFatalError()}, there is chance of a race condition occurring where an event
     * may be counted twice. To avoid this possibility, get the counters before statistics begin to be populated.
     * 
     * @return a counter for {@link #getFatalErrors()}.
     * 
     * @since 4.5
     */
    public ResetOnQueryCounter getFatalErrorsCounter()
    {
        DefaultResetOnQueryCounter counter = new DefaultResetOnQueryCounter();
        fatalErrorsCounters.add(counter);
        counter.add(getFatalErrors());
        return counter;
    }

}