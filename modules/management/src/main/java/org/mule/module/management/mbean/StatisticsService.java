/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import org.mule.api.MuleContext;
import org.mule.management.stats.AllStatistics;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.management.stats.printers.CSVPrinter;
import org.mule.management.stats.printers.HtmlTablePrinter;
import org.mule.management.stats.printers.XMLPrinter;

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
    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        if (muleContext == null)
        {
            stats = new AllStatistics();
        }
        else
        {
            stats = this.muleContext.getStatistics();
        }

    }

    public void clear()
    {
        stats.clear();
    }

    public boolean isEnabled()
    {
        return stats.isEnabled();
    }

    public void setEnabled(boolean b)
    {
        stats.setEnabled(b);

    }

    /**
     * @deprecated use #getServiceStatistics
     */
    @Deprecated
    public Collection<?> getComponentStatistics()
    {
        return stats.getServiceStatistics();
    }

    public Collection<FlowConstructStatistics> getServiceStatistics()
    {
        return stats.getServiceStatistics();
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
