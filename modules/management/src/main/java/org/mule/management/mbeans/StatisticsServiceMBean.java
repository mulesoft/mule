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

import org.mule.management.stats.Statistics;

/**
 * <code>StatisticsServiceMBean</code> is a JMX interfaces for querying Mule event
 * processing statistics
 * 
 * @author <a href="mailto:S.Vanmeerhaege@gfdi.be">Vanmeerhaeghe Stéphane</a>
 * @version $Revision$
 */
public interface StatisticsServiceMBean extends Statistics
{

    void logCSVSummary();

    /**
     * @return
     * @deprecated use getHtmlSummary
     */
    String printHtmlSummary();

    String getHtmlSummary();

    String printXMLSummary();

    String getXMLSummary();
}
