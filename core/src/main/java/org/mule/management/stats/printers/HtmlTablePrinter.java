/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.stats.printers;

import org.mule.util.StringUtils;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * <code>HtmlTablePrinter</code> prints event processing stats as a HTML table
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

    // @Override
    public void print(Collection stats)
    {
        println("<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">");
        String[][] table = getTable(stats);
        boolean providerStats = false;
        println("<tr>");
        for (int i = 0; i < table.length; i++)
        {
            println("<td class=\"statistics\">" + table[i][0] + "</td>");
        }
        println("</tr>");
        for (int i = 1; i < table[0].length; i++)
        {
            println("<tr class=\"" + ((i % 2 == 0) ? "darkline" : "clearline") + "\">");
            for (int j = 0; j < table.length; j++)
            {
                if (j == 0)
                {
                    if (StringUtils.equals(table[1][i], "-"))
                    {
                        if (StringUtils.equals(table[j][i], "By Provider"))
                        {
                            println("<td class=\"statisticsrow\"><div class=\"tablesubheader\">" + table[j][i] + "</div></td>");
                        }
                        else
                        {
                            println("<td class=\"statisticsrow\"><div class=\"tableheader\">" + table[j][i] + "</div></td>");
                        }
                    }
                    else if (StringUtils.isNotEmpty(table[j][i]))
                    {
                        println("<td class=\"statisticsrow\">" + table[j][i] + "</td>");
                    }
                    else
                    {
                        println("<td class=\"statisticsrow\">&nbsp;</td>");
                    }
                }
                else
                {
                    if (providerStats)
                    {
                        println("<td class=\"statisticsrow\">" + getProviderStatsHtml(table[j][i]) + "</td>");
                    }
                    else
                    {
                        println("<td class=\"statisticsrow\">" + ((StringUtils.equals(table[j][i], "-")) ? "" : table[j][i]) + "</td>");
                    }
                }
            }
            println("</tr>");
            if (StringUtils.equals(table[0][i], "By Provider"))
            {
                providerStats = true;
            }
            else
            {
                providerStats = false;
            }
        }
        println("</table>");
    }

    protected String getProviderStatsHtml(String stats)
    {
        if (StringUtils.isBlank(stats))
        {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        buf.append("<table>");
        StringTokenizer st = new StringTokenizer(stats, ";");

        if (st.countTokens() == 0)
        {
            buf.append("<tr><td class=\"statisticssubrow\">");
            int i = stats.indexOf("=");
            buf.append(stats.substring(0, i)).append(": ");
            buf.append("</td><td  class=\"statisticssubrow\">");
            buf.append(stats.substring(i + 1));
            buf.append("</td></tr>");
        }
        else
        {
            String token;
            while (st.hasMoreTokens())
            {
                token = st.nextToken();
                buf.append("<tr><td class=\"statisticssubrow\">");
                int i = token.indexOf("=");
                buf.append(token.substring(0, i)).append(": ");
                buf.append("</td><td class=\"statisticssubrow\">");
                buf.append(token.substring(i + 1));
                buf.append("</td></tr>");
            }
        }
        buf.append("</table>");
        return buf.toString();
    }

}
