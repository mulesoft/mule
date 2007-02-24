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

import org.mule.management.stats.AllStatistics;
import org.mule.management.stats.printers.CSVPrinter;
import org.mule.management.stats.printers.HtmlTablePrinter;
import org.mule.management.stats.printers.XMLPrinter;
import org.mule.umo.UMOManagementContext;

import java.io.StringWriter;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private UMOManagementContext managementContext = null;

    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
        if (managementContext == null)
        {
            stats = new AllStatistics();
        }
        else
        {
            stats = this.managementContext.getStatistics();
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

    public String printCSVSummary ()
    {
        StringWriter w = new StringWriter(2048);
        CSVPrinter printer = new CSVPrinter(w);
        printer.setPrintHeaders(true);
        stats.logSummary(printer);
        return w.toString();
    }

    public String printHtmlSummary ()
    {
        StringWriter w = new StringWriter(8192);
        HtmlTablePrinter printer = new HtmlTablePrinter(w);
        stats.logSummary(printer);
        return w.toString();
    }

    public String printXmlSummary()
    {
        StringWriter w = new StringWriter(8192);
        XMLPrinter printer = new XMLPrinter(w);
        stats.logSummary(printer);
        return w.toString();
    }

}
