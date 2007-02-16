/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import java.io.StringWriter;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.management.stats.AllStatistics;
import org.mule.management.stats.printers.CSVPrinter;
import org.mule.management.stats.printers.HtmlTablePrinter;
import org.mule.management.stats.printers.XMLPrinter;
import org.mule.umo.manager.UMOManager;

/**
 * <code>StatisicsService</code> exposes Mule processing statistics
 */
public class StatisticsService implements StatisticsServiceMBean
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4949499389883146363L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(StatisticsService.class);

    private AllStatistics stats = new AllStatistics();
    private MuleManager manager = null;

    public void setManager(UMOManager manager)
    {
        this.manager = (MuleManager)manager;
        if (manager == null)
        {
            stats = new AllStatistics();
        }
        else
        {
            stats = this.manager.getStatistics();
        }

    }

    /**
     * @see org.mule.management.stats.Statistics#clear()
     */
    public void clear()
    {
        stats.clear();
    }

    /**
     * @see org.mule.management.stats.Statistics#isEnabled()
     */
    public boolean isEnabled()
    {
        return stats.isEnabled();
    }

    /**
     * @see org.mule.management.stats.Statistics#setEnabled(boolean)
     */
    public void setEnabled(boolean b)
    {
        stats.setEnabled(b);

    }

    public Collection getComponentStatistics()
    {
        return stats.getComponentStatistics();
    }

    public void logSummary()
    {
        stats.logSummary();
    }

    public void logCSVSummary()
    {
        CSVPrinter printer = new CSVPrinter(System.out);
        printer.setPrintHeaders(true);
        stats.logSummary(printer);
    }

    /**
     * @return
     * @deprecated Use getHtmlSummary
     */
    public String printHtmlSummary()
    {
        StringWriter w = new StringWriter();
        HtmlTablePrinter printer = new HtmlTablePrinter(w);
        stats.logSummary(printer);
        return w.toString();
    }

    public String getHtmlSummary()
    {
        return printHtmlSummary();
    }

    public String printXMLSummary()
    {
        StringWriter w = new StringWriter();
        XMLPrinter printer = new XMLPrinter(w);
        stats.logSummary(printer);
        return w.toString();
    }

    public String getXMLSummary()
    {
        return printXMLSummary();
    }
}
