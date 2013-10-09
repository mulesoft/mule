/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.mbean;

import org.mule.api.MuleContext;
import org.mule.management.stats.FlowConstructStatistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The MBean for application-wide statistics
 */
public class ApplicationService extends FlowConstructService implements FlowConstructServiceMBean
{
    private static Log LOGGER = LogFactory.getLog(ApplicationService.class);

    public ApplicationService(String type, String name, MuleContext muleContext, FlowConstructStatistics statistics)
    {
        super(type, name, muleContext, statistics);
    }

    @Override
    public void postRegister(Boolean registrationDone)
    {
        try
        {
            statsName = jmxSupport.getObjectName(String.format("%s:type=org.mule.Statistics,%s=%s", objectName.getDomain(), 
                statistics.getFlowConstructType(), jmxSupport.escape(getName())));
            
            // unregister old version if exists
            if (this.server.isRegistered(statsName))
            {
                this.server.unregisterMBean(statsName);
            }

            this.server.registerMBean(new FlowConstructStats(statistics), this.statsName);
        }
        catch (Exception e)
        {
            LOGGER.error("Error post-registering the MBean", e);
        }
    }
}
