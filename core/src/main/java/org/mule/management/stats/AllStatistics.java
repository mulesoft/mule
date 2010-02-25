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

    private Map<String, ServiceStatistics> serviceStats = new HashMap<String, ServiceStatistics>();

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
            printer.print(serviceStats.values());
        }
        else
        {
            for (ServiceStatistics serviceStatistics : serviceStats.values())
            {
                printer.print(serviceStatistics);
            }
        }
        // printer.println("-----------------------------");
        // printer.println("duration (ms): " + (System.currentTimeMillis() -
        // startTime));
    }

    public synchronized void clear()
    {
        for (ServiceStatistics serviceStatistics : getServiceStatistics())
        {
            (serviceStatistics).clear();
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

        for (ServiceStatistics serviceStatistics : serviceStats.values())
        {
            (serviceStatistics).setEnabled(b);
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

    public synchronized void add(ServiceStatistics stat)
    {
        if (stat != null)
        {
            serviceStats.put(stat.getName(), stat);
        }
    }

    public synchronized void remove(ServiceStatistics stat)
    {
        if (stat != null)
        {
            serviceStats.remove(stat.getName());
        }
    }

    /**
     * @deprecated use #getServiceStatistics
     */
    @Deprecated
    public synchronized Collection<ServiceStatistics> getComponentStatistics()
    {
        return serviceStats.values();
    }

    public synchronized Collection<ServiceStatistics> getServiceStatistics()
    {
        return serviceStats.values();
    }
}
