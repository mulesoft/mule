/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

public class FlowConstructStatistics extends AbstractFlowConstructStatistics
{
    private static final long serialVersionUID = 5337576392583767442L;
    private final AtomicLong executionError = new AtomicLong(0);
    private final AtomicLong fatalError = new AtomicLong(0);
    private int threadPoolSize = 0;
    protected final ComponentStatistics flowStatistics = new ComponentStatistics();

    public FlowConstructStatistics(String flowConstructType, String name, int threadPoolSize)
    {
        super(flowConstructType, name);
        this.threadPoolSize = threadPoolSize;
        flowStatistics.setEnabled(enabled);
        if (this.getClass() == FlowConstructStatistics.class)
        {
            clear();
        }
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

}
