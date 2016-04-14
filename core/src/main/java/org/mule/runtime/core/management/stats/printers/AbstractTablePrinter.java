/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats.printers;

import org.mule.management.stats.FlowConstructStatistics;
import org.mule.management.stats.RouterStatistics;
import org.mule.management.stats.SedaServiceStatistics;
import org.mule.management.stats.ServiceStatistics;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>HtmlTablePrinter</code> prints event processing stats as a HTML table
 */
public class AbstractTablePrinter extends PrintWriter
{
    public AbstractTablePrinter(Writer out)
    {
        super(out, true);
    }

    public AbstractTablePrinter(OutputStream out)
    {
        super(out, true);
    }

    public String[] getHeaders()
    {
        String[] column = new String[41];
        column[0] = "Name";
        column[1] = "Service Pool Max Size";
        column[2] = "Service Pool Size";
        column[3] = "Thread Pool Size";
        column[4] = "Current Queue Size";
        column[5] = "Max Queue Size";
        column[6] = "Avg Queue Size";
        column[7] = "Sync Events Received";
        column[8] = "Async Events Received";
        column[9] = "Total Events Received";
        column[10] = "Sync Events Sent";
        column[11] = "Async Events Sent";
        column[12] = "ReplyTo Events Sent";
        column[13] = "Total Events Sent";
        column[14] = "Executed Events";
        column[15] = "Execution Messages";
        column[16] = "Fatal Messages";
        column[17] = "Min Execution Time";
        column[18] = "Max Execution Time";
        column[19] = "Avg Execution Time";
        column[20] = "Total Execution Time";
        column[21] = "Processed Events";
        column[22] = "Min Processing Time";
        column[23] = "Max Processing Time";
        column[24] = "Avg Processing Time";
        column[25] = "Total Processing Time";
        column[26] = "In Router Statistics";
        column[27] = "Total Received";
        column[28] = "Total Routed";
        column[29] = "Not Routed";
        column[30] = "Caught Events";
        column[31] = "By Provider";
        column[32] = "";
        column[33] = "Out Router Statistics";
        column[34] = "Total Received";
        column[35] = "Total Routed";
        column[36] = "Not Routed";
        column[37] = "Caught Events";
        column[38] = "By Provider";
        column[39] = "";
        column[40] = "Sample Period";
        return column;
    }

    protected void getColumn(FlowConstructStatistics stats, String[] col)
    {
        if (stats == null)
        {
            return;
        }
        ServiceStatistics serviceStats = (stats instanceof ServiceStatistics) ? (ServiceStatistics) stats : null;

        Arrays.fill(col, "-");

        col[0] = stats.getName();

        //TODO RM* Handling custom stats objects
        if (stats instanceof SedaServiceStatistics)
        {
            col[1] = ((SedaServiceStatistics) stats).getComponentPoolMaxSize() + "/"
                    + ((SedaServiceStatistics) stats).getComponentPoolAbsoluteMaxSize();
            col[2] = String.valueOf(((SedaServiceStatistics) stats).getComponentPoolSize());
        }
        else
        {
            col[1] = "-";
            col[2] = "-";
        }
        col[3] = String.valueOf(stats.getThreadPoolSize());
        if (serviceStats != null)
        {
            col[4] = String.valueOf(serviceStats.getQueuedEvents());
            col[5] = String.valueOf(serviceStats.getMaxQueueSize());
            col[6] = String.valueOf(serviceStats.getAverageQueueSize());
        }
        col[7] = String.valueOf(stats.getSyncEventsReceived());
        col[8] = String.valueOf(stats.getAsyncEventsReceived());
        col[9] = String.valueOf(stats.getTotalEventsReceived());
        if (serviceStats != null)
        {
            col[10] = String.valueOf(serviceStats.getSyncEventsSent());
            col[11] = String.valueOf(serviceStats.getAsyncEventsSent());
            col[12] = String.valueOf(serviceStats.getReplyToEventsSent());
            col[13] = String.valueOf(serviceStats.getTotalEventsSent());
        }

        if (serviceStats != null)
        {
            col[14] = String.valueOf(serviceStats.getExecutedEvents());
        }
        col[15] = String.valueOf(stats.getExecutionErrors());
        col[16] = String.valueOf(stats.getFatalErrors());
        if (serviceStats != null)
        {
            col[17] = String.valueOf(serviceStats.getMinExecutionTime());
            col[18] = String.valueOf(serviceStats.getMaxExecutionTime());
            col[19] = String.valueOf(serviceStats.getAverageExecutionTime());
            col[20] = String.valueOf(serviceStats.getTotalExecutionTime());
        }

        col[21] = String.valueOf(stats.getProcessedEvents());
        col[22] = String.valueOf(stats.getMinProcessingTime());
        col[23] = String.valueOf(stats.getMaxProcessingTime());
        col[24] = String.valueOf(stats.getAverageProcessingTime());
        col[25] = String.valueOf(stats.getTotalProcessingTime());

        if (serviceStats != null)
        {
            int i = getRouterInfo(serviceStats.getInboundRouterStat(), col, 26);
            i = getRouterInfo(serviceStats.getOutboundRouterStat(), col, i);
        }

        col[40] = String.valueOf(stats.getSamplePeriod());
    }

    protected int getRouterInfo(RouterStatistics stats, String[] col, int index)
    {
        // TODO what's the deal with the +/- signs?
        if (stats.isInbound())
        {
            col[index++] = "-";
        }
        else
        {
            col[index++] = "-";
        }

        col[index++] = String.valueOf(stats.getTotalReceived());
        col[index++] = String.valueOf(stats.getTotalRouted());
        col[index++] = String.valueOf(stats.getNotRouted());
        col[index++] = String.valueOf(stats.getCaughtMessages());

        Map routed = stats.getRouted();

        col[index++] = "-";
        if (!routed.isEmpty())
        {
            Iterator it = routed.entrySet().iterator();

            StringBuilder buf = new StringBuilder(40);
            while (it.hasNext())
            {
                Map.Entry e = (Map.Entry) it.next();
                buf.append(e.getKey()).append('=').append(e.getValue());
                if (it.hasNext())
                {
                    buf.append(';');
                }
            }
            col[index++] = buf.toString();
        }
        else
        {
            col[index++] = "";
        }
        return index;
    }

    protected String[][] getTable(Collection stats)
    {
        String[] cols = getHeaders();
        String[][] table = new String[stats.size() + 1][cols.length];
        for (int i = 0; i < cols.length; i++)
        {
            table[0][i] = cols[i];
        }

        int i = 1;
        for (Iterator iterator = stats.iterator(); iterator.hasNext(); i++)
        {
            getColumn((FlowConstructStatistics) iterator.next(), table[i]);
        }

        return table;
    }

    @Override
    public void print(Object obj)
    {
        if (obj instanceof Collection)
        {
            print((Collection) obj);
        }
        else if (obj instanceof ServiceStatistics)
        {
            List<ServiceStatistics> l = new ArrayList<ServiceStatistics>();
            l.add((ServiceStatistics)obj);
            print(l);
        }
        else
        {
            super.print(obj);
        }
    }

    @Override
    public void println(Object obj)
    {
        print(obj);
        println();
    }

    public void print(Collection c)
    {
        throw new UnsupportedOperationException();
    }

    // help IBM compiler, it complains helplessly about
    // an abmiguously overloaded/overridden method.
    @Override
    public void println(String string)
    {
        this.println((Object) string);
    }

    // help IBM compiler, it complains helplessly about
    // an abmiguously overloaded/overridden method.
    @Override
    public void print(String string)
    {
        this.print((Object) string);
    }
}
