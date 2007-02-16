/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.stats.printers;

import org.mule.management.stats.ComponentStatistics;
import org.mule.management.stats.RouterStatistics;
import org.mule.management.stats.SedaComponentStatistics;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
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
        String[] column = new String[36];
        column[0] = "Component Name";
        column[1] = "Component Pool Max Size";
        column[2] = "Component Pool Size";
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
        column[21] = "In Router Statistics";
        column[22] = "Total Received";
        column[23] = "Total Routed";
        column[24] = "Not Routed";
        column[25] = "Caught Events";
        column[26] = "By Provider";
        column[27] = "";
        column[28] = "Out Router Statistics";
        column[29] = "Total Received";
        column[30] = "Total Routed";
        column[31] = "Not Routed";
        column[32] = "Caught Events";
        column[33] = "By Provider";
        column[34] = "";
        column[35] = "Sample Period";
        return column;
    }

    protected void getColumn(ComponentStatistics stats, String[] col)
    {
        if (stats == null)
        {
            return;
        }


        col[0] = stats.getName();

        //TODO RM* Handling custom stats objects
        if(stats instanceof SedaComponentStatistics)
        {
            col[1] = ((SedaComponentStatistics)stats).getComponentPoolMaxSize() + "/"
                    + ((SedaComponentStatistics)stats).getComponentPoolAbsoluteMaxSize();
            col[2] = String.valueOf(((SedaComponentStatistics)stats).getComponentPoolSize());
        }
        else
        {
            col[1] = "-";
            col[2] = "-";
        }
        col[3] = String.valueOf(stats.getThreadPoolSize());
        col[4] = String.valueOf(stats.getQueuedEvents());
        col[5] = String.valueOf(stats.getMaxQueueSize());
        col[6] = String.valueOf(stats.getAverageQueueSize());
        col[7] = String.valueOf(stats.getSyncEventsReceived());
        col[8] = String.valueOf(stats.getAsyncEventsReceived());
        col[9] = String.valueOf(stats.getTotalEventsReceived());
        col[10] = String.valueOf(stats.getSyncEventsSent());
        col[11] = String.valueOf(stats.getAsyncEventsSent());
        col[12] = String.valueOf(stats.getReplyToEventsSent());
        col[13] = String.valueOf(stats.getTotalEventsSent());
        col[14] = String.valueOf(stats.getExecutedEvents());
        col[15] = String.valueOf(stats.getExecutionErrors());
        col[16] = String.valueOf(stats.getFatalErrors());
        col[17] = String.valueOf(stats.getMinExecutionTime());
        col[18] = String.valueOf(stats.getMaxExecutionTime());
        col[19] = String.valueOf(stats.getAverageExecutionTime());
        col[20] = String.valueOf(stats.getTotalExecutionTime());

        int i = getRouterInfo(stats.getInboundRouterStat(), col, 21);
        i = getRouterInfo(stats.getOutboundRouterStat(), col, i);
        col[i] = String.valueOf(stats.getSamplePeriod());
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

            StringBuffer buf = new StringBuffer(40);
            while (it.hasNext())
            {
                Map.Entry e = (Map.Entry)it.next();
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
            getColumn((ComponentStatistics)iterator.next(), table[i]);
        }

        return table;
    }

    public void print(Object obj)
    {
        if (obj instanceof Collection)
        {
            print((Collection)obj);
        }
        else if (obj instanceof ComponentStatistics)
        {
            List l = new ArrayList();
            l.add(obj);
            print(l);
        }
        else
        {
            super.print(obj);
        }
    }

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
    public void println(String string)
    {
        this.println((Object)string);
    }

    // help IBM compiler, it complains helplessly about
    // an abmiguously overloaded/overridden method.
    public void print(String string)
    {
        this.print((Object)string);
    }

}
