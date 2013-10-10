/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    private ApplicationStatistics appStats;
    private Map<String, FlowConstructStatistics> flowConstructStats = new HashMap<String, FlowConstructStatistics>();

    /**
     * 
     */
    public AllStatistics()
    {
        clear();
        appStats = new ApplicationStatistics(this);
        appStats.setEnabled(isStatisticsEnabled);
        add(appStats);
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
            for (FlowConstructStatistics statistics : flowConstructStats.values())
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
        for (FlowConstructStatistics statistics : getServiceStatistics())
        {
            statistics.clear();
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

        for (FlowConstructStatistics statistics : flowConstructStats.values())
        {
            statistics.setEnabled(b);
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

    public synchronized void add(FlowConstructStatistics stat)
    {
        if (stat != null)
        {
            flowConstructStats.put(stat.getName(), stat);
        }
    }

    public synchronized void remove(FlowConstructStatistics stat)
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
    public synchronized Collection<FlowConstructStatistics> getComponentStatistics()
    {
        return flowConstructStats.values();
    }

    public synchronized Collection<FlowConstructStatistics> getServiceStatistics()
    {
        return flowConstructStats.values();
    }

    public FlowConstructStatistics getApplicationStatistics()
    {
        return appStats;
    }
}
