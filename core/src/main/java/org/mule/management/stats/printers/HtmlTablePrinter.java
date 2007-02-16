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

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

import org.mule.management.stats.RouterStatistics;

/**
 * <code>HtmlTablePrinter</code> prints event processing stats as a HTML table
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class HtmlTablePrinter extends AbstractTablePrinter
{

    public HtmlTablePrinter(Writer out)
    {
        super(out);
    }

    public HtmlTablePrinter(OutputStream out)
    {
        super(out);
    }

    public String[] getHeaders()
    {
        String[] column = new String[32];
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
        column[21] = "Inbound Router Statistics";
        column[22] = "Total Received";
        column[23] = "Total Routed";
        column[24] = "Not Routed";
        column[25] = "Caught Events";
        column[26] = "Outbound Router Statistics";
        column[27] = "Total Received";
        column[28] = "Total Routed";
        column[29] = "Not Routed";
        column[30] = "Caught Events";
        column[31] = "Sample Period";
        return column;
    }

    protected int getRouterInfo(RouterStatistics stats, String[] col, int index)
    {
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

        return index;
    }

    public void print(Collection stats)
    {
        println("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        String[][] table = getTable(stats);
        // boolean endpointStats = false;
        println("<tr>");
        for (int i = 0; i < table.length; i++)
        {
            println("<td class=\"mbeans\">" + table[i][0] + "</td>");
        }
        println("</tr>");
        for (int i = 1; i < table[0].length; i++)
        {
            println("<tr class=\"" + ((i % 2 == 0) ? "darkline" : "clearline") + "\">");
            for (int j = 0; j < table.length; j++)
            {
                if (j == 0 && StringUtils.equals(table[1][i], "-"))
                {
                    println("<td class=\"mbean_row\"><div class=\"tableheader\">" + table[j][i] + "</div></td>");
                }
                else
                {
                    println("<td class=\"mbean_row\">" + ((StringUtils.equals(table[1][i], "-")) ? "" : table[j][i]) + "</td>");
                }
            }
            println("</tr>");
            /*
            boolean bold = false;

            for (int j = 0; j < table.length; j++)
            {
                if (j == 0 || i == 0 || "-".equals(table[j][i]))
                {
                    bold = true;
                    print("<td bgcolor='lightgray'><b>");
                }
                else
                {
                    bold = false;
                    print("<td>");
                }
                if (endpointStats)
                {

                    print(getProviderStatsHtml(table[j][i]));
                }
                else
                {
                    if (endpointStats)
                    {
                        bold = true;
                        print("<b>");
                    }

                    print(("-".equals(table[j][i]) ? "" : table[j][i]));
                }
                print((bold ? "</b>" : "") + "</td>");
            }
            println("</tr>");
            if ("By Provider".equals(table[0][i]))
            {
                endpointStats = true;
            }
            else
            {
                endpointStats = false;
            }
            */
        }
        println("</table>");
    }

    protected String getProviderStatsHtml(String stats)
    {
        if (StringUtils.isEmpty(StringUtils.trim(stats)))
        {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        buf.append("<table>");
        StringTokenizer st = new StringTokenizer(stats, ";");

        if (st.countTokens() == 0)
        {
            buf.append("<tr><td>");
            int i = stats.indexOf("=");
            buf.append(stats.substring(0, i)).append(": ");
            buf.append("</td><td align=\"right'>");
            buf.append(stats.substring(i + 1));
            buf.append("</td></tr>");
        }
        else
        {
            String token;
            while (st.hasMoreTokens())
            {
                token = st.nextToken();
                buf.append("<tr><td>");
                int i = token.indexOf("=");
                buf.append(token.substring(0, i)).append(": ");
                buf.append("</td><td align=''right'>");
                buf.append(token.substring(i + 1));
                buf.append("</td></tr>");
            }
        }
        buf.append("</table>");
        return buf.toString();
    }

}
