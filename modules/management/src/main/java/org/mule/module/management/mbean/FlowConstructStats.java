/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.mbean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.management.stats.FlowConstructStatistics;

import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A concrete class that holds management information for a Mule managed flow.
 */
public class FlowConstructStats extends AbstractFlowConstructStats
{
    private final FlowConstructStatistics statistics;

    /**
     * logger used by this class
     */
    private static Log LOGGER = LogFactory.getLog(FlowConstructStats.class);

    public FlowConstructStats(FlowConstructStatistics statistics)
    {
        super(statistics);
        this.statistics = statistics;
    }

    public long getAverageProcessingTime()
    {
        return statistics.getAverageProcessingTime();
    }

    public long getProcessedEvents()
    {
        return statistics.getProcessedEvents();
    }

    public long getMaxProcessingTime()
    {
        return statistics.getMaxProcessingTime();
    }

    public long getMinProcessingTime()
    {
        return statistics.getMinProcessingTime();
    }

    public long getTotalProcessingTime()
    {
        return statistics.getTotalProcessingTime();
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception
    {
        return super.preRegister(server, name);
    }

    public void postRegister(Boolean registrationDone)
    {
        super.postRegister(registrationDone);
    }

    public void preDeregister() throws Exception
    {
        super.preDeregister();
    }

    public void postDeregister()
    {
        super.postDeregister();
    }

}
