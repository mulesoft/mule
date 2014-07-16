/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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
@Deprecated
public class ServiceStats extends FlowConstructStats implements ServiceStatsMBean, MBeanRegistration
{

    /**
     * logger used by this class
     */
    private static Log LOGGER = LogFactory.getLog(ServiceStats.class);

    private ObjectName inboundName;
    private ObjectName outboundName;

    private final ServiceStatistics statistics;

    public ServiceStats(ServiceStatistics statistics)
    {
        super(statistics);
        this.statistics = statistics;
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

    public long getSyncEventsSent()
    {
        return statistics.getSyncEventsSent();
    }

    public long getTotalEventsSent()
    {
        return statistics.getTotalEventsSent();
    }

    public long getTotalExecutionTime()
    {
        return statistics.getTotalExecutionTime();
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

        try
        {
            RouterStatistics is = statistics.getInboundRouterStat();
            if (is != null)
            {
                String quotedStatsName = ObjectName.quote(statistics.getName());
                inboundName = new ObjectName(name.getDomain() + ":type=org.mule.Statistics,service="
                                             + quotedStatsName + ",router=inbound");

                // unregister old version if exists
                if (server.isRegistered(inboundName))
                {
                    server.unregisterMBean(inboundName);
                }
                server.registerMBean(new RouterStats(is), this.inboundName);
            }

            RouterStatistics os = this.statistics.getOutboundRouterStat();
            if (os != null)
            {
                String quotedStatsName = ObjectName.quote(statistics.getName());
                outboundName = new ObjectName(name.getDomain() + ":type=org.mule.Statistics,service="
                                              + quotedStatsName + ",router=outbound");

                // unregister old version if exists
                if (server.isRegistered(outboundName))
                {
                    server.unregisterMBean(outboundName);
                }
                server.registerMBean(new RouterStats(os), this.outboundName);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error post-registering MBean", e);
        }
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
