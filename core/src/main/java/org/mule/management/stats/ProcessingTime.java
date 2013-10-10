/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.stats;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Accumulates the processing time for all branches of a flow
 */
public class ProcessingTime implements Serializable
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = 1L;

    private AtomicLong accumulator = new AtomicLong();
    private FlowConstructStatistics statistics;

    /**
     * Create a ProcessingTime for the specified MuleSession.
     *
     * @return ProcessingTime if the session has an enabled FlowConstructStatistics or null otherwise
     */
    public static ProcessingTime newInstance(MuleEvent event)
    {
        if (event != null)
        {
            FlowConstruct fc = event.getFlowConstruct();
            if (fc != null)
            {
                FlowConstructStatistics stats = fc.getStatistics();
                if (stats != null && fc.getStatistics().isEnabled())
                {
                    return new ProcessingTime(stats, event.getMuleContext());
                }
            }
        }
        return null;
    }

    /**
     * Create a Processing Time
     *
     * @param stats       never null
     * @param muleContext
     */
    private ProcessingTime(FlowConstructStatistics stats, MuleContext muleContext)
    {
        this.statistics = stats;
        ProcessingTimeWatcher processorTimeWatcher = muleContext.getProcessorTimeWatcher();
        processorTimeWatcher.addProcessingTime(this);
    }

    /**
     * Add the execution time for this branch to the flow construct's statistics
     *
     * @param startTime time this branch started
     */
    public void addFlowExecutionBranchTime(long startTime)
    {
        if (statistics.isEnabled())
        {
            long elapsedTime = getEffectiveTime(System.currentTimeMillis() - startTime);
            statistics.addFlowExecutionBranchTime(elapsedTime, accumulator.addAndGet(elapsedTime));
        }
    }

    /**
     * Convert processing time to effective processing time.  If processing took less than a tick, we consider
     * it to have been one millisecond
     */
    public static long getEffectiveTime(long time)
    {
        return (time <= 0) ? 1L : time;
    }

    public FlowConstructStatistics getStatistics()
    {
        return statistics;
    }

    public AtomicLong getAccumulator()
    {
        return accumulator;
    }
}
