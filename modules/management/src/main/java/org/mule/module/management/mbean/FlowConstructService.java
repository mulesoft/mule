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
import org.mule.api.MuleContext;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.management.stats.FlowConstructStatistics;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistration;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * <code>FlowConstructService</code> exposes service information about a Mule Managed
 * flow construct.
 */
public class FlowConstructService extends AbstractFlowConstructService implements FlowConstructServiceMBean, MBeanRegistration, FlowConstructStatsMBean
{
    private static Log LOGGER = LogFactory.getLog(FlowConstructService.class);

    protected FlowConstructStatistics statistics;

    protected MuleContext muleContext;

    public FlowConstructService(String type, String name, MuleContext muleContext, FlowConstructStatistics statistics)
    {
        super(type, name, muleContext);
        this.statistics = statistics;
    }

    public ObjectName getStatistics()
    {
        return statsName;
    }

    public void clearStatistics()
    {
        statistics.clear();
    }

    public long getAsyncEventsReceived()
    {
        return statistics.getAsyncEventsReceived();
    }

    public long getSyncEventsReceived()
    {
        return statistics.getSyncEventsReceived();
    }
   
    public long getTotalEventsReceived()
    {
        return statistics.getTotalEventsReceived();
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

    @Override
    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception
    {
        return super.preRegister(server, name);
    }

    @Override
    public void postRegister(Boolean registrationDone)
    {
        super.postRegister(registrationDone);
    }

    @Override
    public void preDeregister() throws Exception
    {
        super.preDeregister();
    }

    @Override
    public void postDeregister()
    {
        super.postDeregister();
    }
}
