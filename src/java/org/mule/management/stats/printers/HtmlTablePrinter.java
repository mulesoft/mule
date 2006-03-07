/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.stats.printers;

import org.apache.commons.lang.StringUtils;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.StringTokenizer;

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

    public void print(Collection stats)
    {
        println("<font size='8'><table valign='top'>");
        String[][] table = getTable(stats);
        boolean endpointStats = false;
        for (int i = 0; i < table[0].length; i++) {
            println("<tr valign='top'>");
            boolean bold = false;

            for (int j = 0; j < table.length; j++) {
                if (j == 0 || i == 0 || "-".equals(table[j][i])) {
                    bold = true;
                    print("<td bgcolor='lightgray'><b>");
                } else {
                    bold = false;
                    print("<td>");
                }
                if (endpointStats) {

                    print(getProviderStatsHtml(table[j][i]));
                } else {
                    if (endpointStats) {
                        bold = true;
                        print("<b>");
                    }

                    print(("-".equals(table[j][i]) ? "" : table[j][i]));
                }
                print((bold ? "</b>" : "") + "</td>");
            }
            println("</tr>");
            if ("By Provider".equals(table[0][i])) {
                endpointStats = true;
            } else {
                endpointStats = false;
            }
        }
        println("</table></font>");
    }

    protected String getProviderStatsHtml(String stats)
    {
        if (StringUtils.isEmpty(StringUtils.trim(stats))) {
            return "";
        }

        StringBuffer buf = new StringBuffer();
        buf.append("<table>");
        StringTokenizer st = new StringTokenizer(stats, ";");

        if (st.countTokens() == 0) {
            buf.append("<tr><td>");
            int i = stats.indexOf("=");
            buf.append(stats.substring(0, i)).append(": ");
            buf.append("</td><td align=\"right'>");
            buf.append(stats.substring(i + 1));
            buf.append("</td></tr>");
        } else {
            String token;
            while (st.hasMoreTokens()) {
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
