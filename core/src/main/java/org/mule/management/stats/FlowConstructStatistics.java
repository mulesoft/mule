/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.stats;

import org.mule.api.management.stats.Statistics;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicLong;

public class FlowConstructStatistics extends AbstractFlowConstructStatistics
{
    private static final long serialVersionUID = 5337576392583767442L;

    protected final ComponentStatistics flowStatistics = new ComponentStatistics();

    public FlowConstructStatistics(String flowConstructType, String name)
    {
        super(flowConstructType, name);
        flowStatistics.setEnabled(enabled);
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
        super.setEnabled(b);
        flowStatistics.setEnabled(enabled);
    }

    public synchronized void clear()
    {
        super.clear();
        if (flowStatistics != null)
        {
            flowStatistics.clear();
        }
    }

    public void addFlowExecutionTime(long time)
    {
        flowStatistics.addExecutionTime(time);
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
}
