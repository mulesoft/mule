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

import org.mule.management.stats.printers.AbstractTablePrinter;
import org.mule.management.stats.printers.SimplePrinter;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <code>AllStatistics</code> TODO
 */
public class AllStatistics
{
    private boolean isStatisticsEnabled;
    private long startTime;

    private Map<String, AbstractFlowConstructStatistics> flowConstructStats = new HashMap<String, AbstractFlowConstructStatistics>();

    /**
     * 
     */
    public AllStatistics()
    {
        clear();
    }

    public void logSummary()
    {
        logSummary(new SimplePrinter(System.out));
    }

    public void logSummary(PrintWriter printer)
    {

        if (printer instanceof AbstractTablePrinter)
        {
            printer.print(flowConstructStats.values());
        }
        else
        {
            for (AbstractFlowConstructStatistics statistics : flowConstructStats.values())
            {
                printer.print(statistics);
            }
        }
        // printer.println("-----------------------------");
        // printer.println("duration (ms): " + (System.currentTimeMillis() -
        // startTime));
    }

    public synchronized void clear()
    {
        for (AbstractFlowConstructStatistics statistics : getServiceStatistics())
        {
            (statistics).clear();
        }
        startTime = System.currentTimeMillis();
    }

    /**
     * Are statistics logged
     */
    public boolean isEnabled()
    {
        return isStatisticsEnabled;
    }

    /**
     * Enable statistics logs (this is a dynamic parameter)
     */
    public void setEnabled(boolean b)
    {
        isStatisticsEnabled = b;

        for (AbstractFlowConstructStatistics statistics : flowConstructStats.values())
        {
            (statistics).setEnabled(b);
        }
    }

    public synchronized long getStartTime()
    {
        return startTime;
    }

    public synchronized void setStartTime(long startTime)
    {
        this.startTime = startTime;
    }

    public synchronized void add(AbstractFlowConstructStatistics stat)
    {
        if (stat != null)
        {
            flowConstructStats.put(stat.getName(), stat);
        }
    }

    public synchronized void remove(AbstractFlowConstructStatistics stat)
    {
        if (stat != null)
        {
            flowConstructStats.remove(stat.getName());
        }
    }

    /**
     * @deprecated use #getServiceStatistics
     */
    @Deprecated
    public synchronized Collection<AbstractFlowConstructStatistics> getComponentStatistics()
    {
        return flowConstructStats.values();
    }

    public synchronized Collection<AbstractFlowConstructStatistics> getServiceStatistics()
    {
        return flowConstructStats.values();
    }
}
