/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
