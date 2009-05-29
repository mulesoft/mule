/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.management.mbean;

import org.mule.management.stats.RouterStatistics;
import org.mule.management.stats.ServiceStatistics;

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ServiceStats</code> TODO
 */
public class ServiceStats implements ServiceStatsMBean, MBeanRegistration
{

    /**
     * logger used by this class
     */
    private static Log LOGGER = LogFactory.getLog(ServiceStats.class);

    private MBeanServer server;

    private ObjectName name;
    private ObjectName inboundName;
    private ObjectName outboundName;

    private ServiceStatistics statistics;

    public ServiceStats(ServiceStatistics statistics)
    {
        this.statistics = statistics;
    }

    public void clearStatistics()
    {
        statistics.clear();
    }

    public long getAsyncEventsReceived()
    {
        return statistics.getAsyncEventsReceived();
    }

    public long getAsyncEventsSent()
    {
        return statistics.getAsyncEventsSent();
    }

    public long getAverageExecutionTime()
    {
        return statistics.getAverageExecutionTime();
    }

    public long getAverageQueueSize()
    {
        return statistics.getAverageQueueSize();
    }

    public long getExecutedEvents()
    {
        return statistics.getExecutedEvents();
    }

    public long getExecutionErrors()
    {
        return statistics.getExecutionErrors();
    }

    public long getFatalErrors()
    {
        return statistics.getFatalErrors();
    }

    public long getMaxExecutionTime()
    {
        return statistics.getMaxExecutionTime();
    }

    public long getMaxQueueSize()
    {
        return statistics.getMaxQueueSize();
    }

    public long getMinExecutionTime()
    {
        return statistics.getMinExecutionTime();
    }

    public String getName()
    {
        return statistics.getName();
    }

    public long getQueuedEvents()
    {
        return statistics.getQueuedEvents();
    }

    public long getReplyToEventsSent()
    {
        return statistics.getReplyToEventsSent();
    }

    public long getSyncEventsReceived()
    {
        return statistics.getSyncEventsReceived();
    }

    public long getSyncEventsSent()
    {
        return statistics.getSyncEventsSent();
    }

    public long getTotalEventsReceived()
    {
        return statistics.getTotalEventsReceived();
    }

    public long getTotalEventsSent()
    {
        return statistics.getTotalEventsSent();
    }

    public long getTotalExecutionTime()
    {
        return statistics.getTotalExecutionTime();
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception
    {
        this.server = server;
        this.name = name;
        return name;
    }

    public void postRegister(Boolean registrationDone)
    {

        try
        {
            RouterStatistics is = this.statistics.getInboundRouterStat();
            if (is != null)
            {
                inboundName = new ObjectName(name.getDomain() + ":type=org.mule.Statistics,service="
                                             + statistics.getName() + ",router=inbound");
                // unregister old version if exists
                if (this.server.isRegistered(inboundName))
                {
                    this.server.unregisterMBean(inboundName);
                }
                this.server.registerMBean(new RouterStats(is), this.inboundName);
            }
            RouterStatistics os = this.statistics.getOutboundRouterStat();
            if (os != null)
            {
                outboundName = new ObjectName(name.getDomain() + ":type=org.mule.Statistics,service="
                                              + statistics.getName() + ",router=outbound");
                // unregister old version if exists
                if (this.server.isRegistered(outboundName))
                {
                    this.server.unregisterMBean(outboundName);
                }
                this.server.registerMBean(new RouterStats(os), this.outboundName);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error post-registering MBean", e);
        }
    }

    public void preDeregister() throws Exception
    {
        // nothing to do
    }

    public void postDeregister()
    {
        try
        {
            if (this.server.isRegistered(inboundName))
            {
                this.server.unregisterMBean(inboundName);
            }
        }
        catch (Exception ex)
        {
            LOGGER.error("Error unregistering ServiceStats child " + inboundName.getCanonicalName(), ex);
        }
        try
        {
            if (this.server.isRegistered(outboundName))
            {
                this.server.unregisterMBean(outboundName);
            }
        }
        catch (Exception ex)
        {
            LOGGER.error("Error unregistering ServiceStats child " + inboundName.getCanonicalName(), ex);
        }
    }

    public ObjectName getRouterInbound()
    {
        return this.inboundName;
    }

    public ObjectName getRouterOutbound()
    {
        return this.outboundName;
    }
}
