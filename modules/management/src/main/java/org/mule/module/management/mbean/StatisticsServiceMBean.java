/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.mbean;

import org.mule.api.management.stats.Statistics;

/**
 * <code>StatisticsServiceMBean</code> is a JMX interfaces for querying Mule event
 * processing statistics.
 */
public interface StatisticsServiceMBean extends Statistics
{
    String DEFAULT_JMX_NAME = "type=Statistics,name=AllStatistics";

    String printCSVSummary ();

    String printHtmlSummary ();

    String printXmlSummary ();
}
