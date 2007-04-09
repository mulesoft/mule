
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
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;


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
        String[] column = new String[42];
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
        column[27] = "Providers";
        column[28] = "";
        column[29] = "Providers";
        column[30] = "Router";
        column[31] = "Router";
        column[32] = "Type";
        column[33] = "Total Received";
        column[34] = "Total Routed";
        column[35] = "Not Routed";
        column[36] = "Caught Events";
        column[37] = "Providers";
        column[38] = "";
        column[39] = "Providers";
        column[40] = "Router";
        column[41] = "Sample Period";
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

        index++;
        Map routed = stats.getRouted();
        if (!routed.isEmpty())
        {
            Iterator it = routed.entrySet().iterator();

            StringBuffer buf = new StringBuffer(40);
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
        index += 2;

        return index;
    }

    public void print(Collection stats)
    {
        println("<?xml version=\"1.0\" encoding=\"US-ASCII\"?>");
        println("<Components>");
        String[][] table = getTable(stats);
        boolean router = false;
        boolean providers = false;

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
                else if (StringUtils.equals(table[0][j], "Providers"))
                {
                    if (StringUtils.isEmpty(table[i][j + 1]) && StringUtils.equals(table[0][j + 2], "Providers"))
                    {
                        println("<Providers/>", indentLevel);
                        j += 2;
                    }
                    else
                    {
                        if (!providers)
                        {
                            println("<Providers>", indentLevel);
                            indentLevel++;
                            providers = true;
                        }
                        else
                        {
                            indentLevel--;
                            println("</Providers>", indentLevel);
                            providers = false;
                        }
                    }
                }
                else
                {
                    if (providers)
                    {
                        printProviderStatsXml(table[i][j], indentLevel);
                    }
                    else
                    {
                        println("<Statistic name=\"" + table[0][j] + "\" value=\"" + table[i][j] + "\"/>",
                                indentLevel);
                    }
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

    protected void printProviderStatsXml(String stats, int indentLevel)
    {
        if (StringUtils.isBlank(stats))
        {
            return;
        }

        StringTokenizer st = new StringTokenizer(stats, ";");

        if (st.countTokens() == 0)
        {
            StringBuffer buf = new StringBuffer();
            buf.append("<Provider name=\"");
            int i = stats.indexOf("=");
            buf.append(stats.substring(0, i));
            buf.append("\" value=\"");
            buf.append(stats.substring(i + 1));
            buf.append("\"/>");
            println(buf.toString(), indentLevel);
        }
        else
        {
            String token;
            while (st.hasMoreTokens())
            {
                StringBuffer buf = new StringBuffer();
                token = st.nextToken();
                buf.append("<Provider name=\"");
                int i = token.indexOf("=");
                buf.append(token.substring(0, i));
                buf.append("\" value=\"");
                buf.append(token.substring(i + 1));
                buf.append("\"/>");
                println(buf.toString(), indentLevel);
            }
        }
    }
}
