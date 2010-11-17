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

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * <code>AbstractFlowConstructService</code> exposes information common to services and flows.
 */
public abstract class AbstractFlowConstructService implements MBeanRegistration
{
    private static Log LOGGER = LogFactory.getLog(AbstractFlowConstructService.class);

    protected MBeanServer server;

    protected String name;

    protected String type;

    protected ObjectName statsName;

    protected ObjectName objectName;

    protected MuleContext muleContext;


    protected AbstractFlowConstructService(String type, String name, MuleContext muleContext)
    {
        this.muleContext = muleContext;
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public ObjectName getStatistics()
    {
        return statsName;
    }

    public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception
    {
        this.server = server;
        this.objectName = name;
        return name;
    }

    public void postRegister(Boolean registrationDone)
    {
        AbstractFlowConstruct flow = muleContext.getRegistry().lookupObject(getName());
        try
        {
            if (flow.getStatistics() != null)
            {
                statsName = new ObjectName(objectName.getDomain() + ":type=org.mule.Statistics," +
                    flow.getConstructType() + "=" + getName());
                // unregister old version if exists
                if (this.server.isRegistered(statsName))
                {
                    this.server.unregisterMBean(statsName);
                }

                this.server.registerMBean(new FlowConstructStats(flow.getStatistics()), this.statsName);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Error post-registering the MBean", e);
        }
    }

    public void preDeregister() throws Exception
    {
        try
        {
            if (this.server.isRegistered(statsName))
            {
                this.server.unregisterMBean(statsName);
            }
        }
        catch (Exception ex)
        {
            LOGGER.error("Error unregistering ServiceService child " + statsName.getCanonicalName(), ex);
        }
    }

    public void postDeregister()
    {
        // nothing to do
    }

}