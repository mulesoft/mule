/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.management.i18n.ManagementMessages;
import org.mule.module.management.mbean.YourKitProfilerService;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;
import org.mule.util.ClassUtils;

import java.util.Collections;
import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class YourKitProfilerAgent implements Agent, MuleContextAware
{
    /**
     * MBean name to register under.
     */
    public static final String PROFILER_OBJECT_NAME = "name=Profiler";

    private String name = "yourkit-profiler";
    private MBeanServer mBeanServer;
    private ObjectName profilerName;

    private JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    private JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();

    /**
     * Logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(YourKitProfilerAgent.class);

    protected MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return "Profiler JMX Agent";
    }

    public List<Class<? extends Agent>> getDependentAgents()
    {
        return Collections.emptyList();
    }

    public void initialise() throws InitialisationException
    {
        if(!isApiAvailable())
        {
            logger.warn("Cannot find YourKit API. Profiler JMX Agent will be unregistered.");
            unregisterMeQuietly();
            return;
        }

        final List servers = MBeanServerFactory.findMBeanServer(null);
        if(servers.isEmpty())
        {
            throw new InitialisationException(ManagementMessages.noMBeanServerAvailable(), this);
        }

        try
        {
            mBeanServer = (MBeanServer) servers.get(0);

            profilerName = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":" + PROFILER_OBJECT_NAME);

            // unregister existing YourKit MBean first if required
            unregisterMBeansIfNecessary();
            mBeanServer.registerMBean(new YourKitProfilerService(), profilerName);
        }
        catch(Exception e)
        {
            throw new InitialisationException(CoreMessages.failedToStart(this.getName()), e, this);
        }
    }

    /**
     * Unregister Profiler MBean if there are any left over the old deployment
     */
    protected void unregisterMBeansIfNecessary()
            throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException
    {
        if(mBeanServer == null || profilerName == null)
        {
            return;
        }
        if(mBeanServer.isRegistered(profilerName))
        {
            mBeanServer.unregisterMBean(profilerName);
        }
    }

    /**
     * Quietly unregister ourselves.
     */
    protected void unregisterMeQuietly()
    {
        try
        {
            // remove the agent from the list, it's not functional
            muleContext.getRegistry().unregisterAgent(this.getName());
        }
        catch (MuleException e)
        {
            // not interested, really
        }
    }

    private boolean isApiAvailable()
    {
        try{
            ClassUtils.getClass("com.yourkit.api.Controller");
            return true;
        }
        catch(ClassNotFoundException e)
        {
            return false;
        }
    }

    public void start() throws MuleException
    {
        // nothing to do
    }

    public void stop() throws MuleException
    {
        // nothing to do
    }

    public void dispose()
    {
        try
        {
            unregisterMBeansIfNecessary();
        }
        catch (Exception e)
        {
            logger.error("Couldn't unregister MBean: "
                         + (profilerName != null ? profilerName.getCanonicalName() : "null"), e);
        }
    }

}
