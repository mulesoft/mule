/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.mbeans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.management.stats.AllStatistics;
import org.mule.management.stats.printers.CSVPrinter;
import org.mule.management.stats.printers.HtmlTablePrinter;
import org.mule.umo.manager.UMOManager;

import java.io.StringWriter;
import java.util.Collection;

/**
 * <code>StatisicsService</code> exposes Mule processing statistics
 * 
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stéphane </a>
 * @version $Revision$
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
    protected static transient Log logger = LogFactory.getLog(StatisticsService.class);

    private AllStatistics stats = new AllStatistics();
    private MuleManager manager = null;

    public void setManager(UMOManager manager)
    {
        this.manager = (MuleManager) manager;
        if (manager == null) {
            stats = new AllStatistics();
        } else {
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
     *
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

    public String getHtmlSummary() {
        return printHtmlSummary();
    }
}
