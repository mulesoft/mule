/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.management.stats.printers;

import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.management.stats.RouterStatistics;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>HtmlTablePrinter</code> prints event processing stats as a HTML table
 */
public class AbstractTablePrinter extends PrintWriter {

  public AbstractTablePrinter(Writer out) {
    super(out, true);
  }

  public AbstractTablePrinter(OutputStream out) {
    super(out, true);
  }

  public String[] getHeaders() {
    String[] column = new String[41];
    int i = 0;
    column[i++] = "Name";
    column[i++] = "Thread Pool Size";
    column[i++] = "Current Queue Size";
    column[i++] = "Max Queue Size";
    column[i++] = "Avg Queue Size";
    column[i++] = "Sync Events Received";
    column[i++] = "Async Events Received";
    column[i++] = "Total Events Received";
    column[i++] = "Sync Events Sent";
    column[i++] = "Async Events Sent";
    column[i++] = "ReplyTo Events Sent";
    column[i++] = "Total Events Sent";
    column[i++] = "Executed Events";
    column[i++] = "Execution Messages";
    column[i++] = "Fatal Messages";
    column[i++] = "Min Execution Time";
    column[i++] = "Max Execution Time";
    column[i++] = "Avg Execution Time";
    column[i++] = "Total Execution Time";
    column[i++] = "Processed Events";
    column[i++] = "Min Processing Time";
    column[i++] = "Max Processing Time";
    column[i++] = "Avg Processing Time";
    column[i++] = "Total Processing Time";
    column[i++] = "In Router Statistics";
    column[i++] = "Total Received";
    column[i++] = "Total Routed";
    column[i++] = "Not Routed";
    column[i++] = "Caught Events";
    column[i++] = "By Provider";
    column[i++] = "";
    column[i++] = "Out Router Statistics";
    column[i++] = "Total Received";
    column[i++] = "Total Routed";
    column[i++] = "Not Routed";
    column[i++] = "Caught Events";
    column[i++] = "By Provider";
    column[i++] = "";
    column[i++] = "Sample Period";
    return column;
  }

  protected void getColumn(FlowConstructStatistics stats, String[] col) {
    if (stats == null) {
      return;
    }

    Arrays.fill(col, "-");

    int j = 0;
    col[j++] = stats.getName();

    // TODO RM* Handling custom stats objects
    j += 4;
    col[j++] = String.valueOf(stats.getTotalEventsReceived());
    j += 4;

    j++;
    col[j++] = String.valueOf(stats.getExecutionErrors());
    col[j++] = String.valueOf(stats.getFatalErrors());
    j += 4;

    col[j++] = String.valueOf(stats.getProcessedEvents());
    col[j++] = String.valueOf(stats.getMinProcessingTime());
    col[j++] = String.valueOf(stats.getMaxProcessingTime());
    col[j++] = String.valueOf(stats.getAverageProcessingTime());
    col[j++] = String.valueOf(stats.getTotalProcessingTime());

    col[j++] = String.valueOf(stats.getSamplePeriod());
  }

  protected int getRouterInfo(RouterStatistics stats, String[] col, int index) {
    // TODO what's the deal with the +/- signs?
    if (stats.isInbound()) {
      col[index++] = "-";
    } else {
      col[index++] = "-";
    }

    col[index++] = String.valueOf(stats.getTotalReceived());
    col[index++] = String.valueOf(stats.getTotalRouted());
    col[index++] = String.valueOf(stats.getNotRouted());
    col[index++] = String.valueOf(stats.getCaughtMessages());

    Map routed = stats.getRouted();

    col[index++] = "-";
    if (!routed.isEmpty()) {
      Iterator it = routed.entrySet().iterator();

      StringBuilder buf = new StringBuilder(40);
      while (it.hasNext()) {
        Map.Entry e = (Map.Entry) it.next();
        buf.append(e.getKey()).append('=').append(e.getValue());
        if (it.hasNext()) {
          buf.append(';');
        }
      }
      col[index++] = buf.toString();
    } else {
      col[index++] = "";
    }
    return index;
  }

  protected String[][] getTable(Collection stats) {
    String[] cols = getHeaders();
    String[][] table = new String[stats.size() + 1][cols.length];
    for (int i = 0; i < cols.length; i++) {
      table[0][i] = cols[i];
    }

    int i = 1;
    for (Iterator iterator = stats.iterator(); iterator.hasNext(); i++) {
      getColumn((FlowConstructStatistics) iterator.next(), table[i]);
    }

    return table;
  }

  @Override
  public void print(Object obj) {
    if (obj instanceof Collection) {
      print((Collection) obj);
    } else {
      super.print(obj);
    }
  }

  @Override
  public void println(Object obj) {
    print(obj);
    println();
  }

  public void print(Collection c) {
    throw new UnsupportedOperationException();
  }

  // help IBM compiler, it complains helplessly about
  // an abmiguously overloaded/overridden method.
  @Override
  public void println(String string) {
    this.println((Object) string);
  }

  // help IBM compiler, it complains helplessly about
  // an abmiguously overloaded/overridden method.
  @Override
  public void print(String string) {
    this.print((Object) string);
  }
}
