
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

import org.mule.management.stats.RouterStatistics;
import org.mule.util.StringUtils;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;


/**
 * <code>XMLPrinter</code> prints event processing stats as a XML document
 * 
 */
public class XMLPrinter extends AbstractTablePrinter
{
    /**
     * Indentation step for XML pretty-printing.
     */
    protected static final int XML_INDENT_SIZE = 2;

    public XMLPrinter(Writer out)
    {
        super(out);
    }

    public XMLPrinter(OutputStream out)
    {
        super(out);
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
        column[21] = "Router";
        column[22] = "Type";
        column[23] = "Total Received";
        column[24] = "Total Routed";
        column[25] = "Not Routed";
        column[26] = "Caught Events";
        column[27] = "Router";
        column[28] = "Router";
        column[29] = "Type";
        column[30] = "Total Received";
        column[31] = "Total Routed";
        column[32] = "Not Routed";
        column[33] = "Caught Events";
        column[34] = "Router";
        column[35] = "Sample Period";
        return column;
    }
    
    protected int getRouterInfo(RouterStatistics stats, String[] col, int index)
    {
        index++;
        if (stats.isInbound())
        {
            col[index++] = "Inbound";
        }
        else
        {
            col[index++] = "Outbound";
        }

        col[index++] = String.valueOf(stats.getTotalReceived());
        col[index++] = String.valueOf(stats.getTotalRouted());
        col[index++] = String.valueOf(stats.getNotRouted());
        col[index++] = String.valueOf(stats.getCaughtMessages());

        return ++index;
    }

    public void print(Collection stats)
    {
        println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>");
        println("<Components>");
        String[][] table = getTable(stats);
        boolean router = false;

        int indentLevel = 1;

        for (int i = 1; i < table.length; i++)
        {
            println("<Component name=\"" + table[i][0] + "\">", indentLevel);
            indentLevel++;
            for (int j = 1; j < table[i].length; j++)
            {
                if (StringUtils.equals(table[0][j], "Router"))
                {
                    if (!router)         
                    {
                        println("<Router type=\"" + table[i][++j] + "\">", indentLevel);
                        indentLevel++;
                        router = true;
                    }
                    else
                    {
                        indentLevel--;
                        println("</Router>", indentLevel);
                        router = false;
                    }
                }
                else
                {
                    println("<Statistic name=\"" + table[0][j] + "\" value=\"" + table[i][j] + "\"/>",
                            indentLevel);
                }
            }
            indentLevel--;
            println("</Component>", indentLevel);
        }
        indentLevel--;
        println("</Components>", indentLevel);
    }

    public void println(String s, int indentLevel)
    {
        final String indent = StringUtils.repeat(' ', indentLevel * XML_INDENT_SIZE);
        println(indent + s);
    }
}
