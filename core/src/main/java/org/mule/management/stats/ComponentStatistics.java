/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import org.mule.api.management.stats.Statistics;
import org.mule.management.stats.printers.SimplePrinter;
import org.mule.util.StringUtils;

import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 */
public class ComponentStatistics implements Statistics
{

    private static final Log logger = LogFactory.getLog(ComponentStatistics.class);

    /**
     * Serial version
     */
    private static final long serialVersionUID = -2086999226732861674L;

    private long minExecutionTime = 0;
    private long maxExecutionTime = 0;
    private long averageExecutionTime = 0;
    private long executedEvent = 0;
    private long totalExecTime = 0;
    private boolean enabled = false;
    private long intervalTime = 0;
    private long currentIntervalStartTime = 0;
    private boolean statIntervalTimeEnabled = false;

    /**
     * The constructor added to initialize the interval time in ms that stats   
     * are measured for from the property statIntervalTime. If the property is 
     * not set or cannot be parsed, disable interval time and just compute 
     * stats from start of mule.
     * TODO: The code to create and use an interval time for measuring average execution 
     * time could be removed once a complete solution is available in MuleHQ to
     * monitor this
     */
    public ComponentStatistics() 
    {
        String intervalTimeString = System.getProperty("statIntervalTime");
        if (StringUtils.isBlank(intervalTimeString))
        {
            statIntervalTimeEnabled = false;
        }
        else
        {
            try
            {
                intervalTime = Integer.parseInt(intervalTimeString);
                statIntervalTimeEnabled = true;
            }
            catch (NumberFormatException e)
            {
                // just disable it
                statIntervalTimeEnabled = false;
                logger.warn("Couldn't parse statIntervalTime: " + intervalTimeString + ". Disabled.");
            }
        }
    }

    public void clear()
    {
        minExecutionTime = 0;
        maxExecutionTime = 0;
        executedEvent = 0;
        totalExecTime = 0;
        averageExecutionTime = 0;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void logSummary()
    {
        logSummary(new SimplePrinter(System.out));
    }

    public void logSummary(PrintWriter printer)
    {
        printer.print(this);
    }

    public void setEnabled(boolean b)
    {
        this.enabled = b;
    }

    public long getMaxExecutionTime()
    {
        return maxExecutionTime;
    }

    public long getMinExecutionTime()
    {
        return minExecutionTime;
    }

    public long getTotalExecutionTime()
    {
        return totalExecTime;
    }

    /*
     * executedEvents is since interval started
     */
    public long getExecutedEvents()
    {
        return executedEvent;
    }

    /**
     * Add a new execution-time measurement for one branch of processing an event
     * @param first true if this is the first branch for this event
     * @param branch the time to execute this branch
     * @param total the total time (so far) for  processing this event
     */
    public synchronized void addExecutionBranchTime(boolean first, long branch, long total)
    {
        if (statIntervalTimeEnabled)
        {
            long currentTime = System.currentTimeMillis();
            if (currentIntervalStartTime == 0)
            {
                currentIntervalStartTime = currentTime;
            }

            if ((currentTime - currentIntervalStartTime) > intervalTime)
            {
                clear();
                currentIntervalStartTime = currentTime;
            }
        }

        if (first)
        {
            executedEvent++;
        }

        totalExecTime += ProcessingTime.getEffectiveTime(branch);
        long effectiveTotal = ProcessingTime.getEffectiveTime(total);
        if (maxExecutionTime == 0 || effectiveTotal > maxExecutionTime)
        {
            maxExecutionTime = effectiveTotal;
        }
        averageExecutionTime = Math.round(totalExecTime / executedEvent);
    }

    /**
     * Add the complete execution time for a flow that also reports branhc execution times
     */
    public synchronized void addCompleteExecutionTime(long time)
    {
        long effectiveTime = ProcessingTime.getEffectiveTime(time);
        if (minExecutionTime == 0 || effectiveTime < minExecutionTime)
        {
            minExecutionTime = effectiveTime;
        }
    }

    /**
     * Add a new execution-time measurement for processing an event.
     *
     * @param time
     */
    public synchronized void addExecutionTime(long time)
    {
        if (statIntervalTimeEnabled)
        {
            long currentTime = System.currentTimeMillis();
            if (currentIntervalStartTime == 0)
            {
                currentIntervalStartTime = currentTime;
            }

            if ((currentTime - currentIntervalStartTime) > intervalTime)
            {
                clear();
                currentIntervalStartTime = currentTime;
            }
        }

        executedEvent++;

        long effectiveTime = ProcessingTime.getEffectiveTime(time);
        totalExecTime += effectiveTime;

        if (minExecutionTime == 0 || effectiveTime < minExecutionTime)
        {
            minExecutionTime = time;
        }
        if (maxExecutionTime == 0 || effectiveTime > maxExecutionTime)
        {
            maxExecutionTime = time;
        }
        averageExecutionTime = Math.round(totalExecTime / executedEvent);
    }

    public long getAverageExecutionTime()
    {
        return averageExecutionTime;
    }

}
