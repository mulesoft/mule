/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.mule.MuleServer;
import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.management.i18n.ManagementMessages;
import org.mule.management.mbeans.YourKitProfilerService;
import org.mule.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.management.support.JmxSupport;
import org.mule.management.support.JmxSupportFactory;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;

import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class YourKitProfilerAgent implements UMOAgent
{
    /**
     * MBean name to register under.
     */
    public static final String PROFILER_OBJECT_NAME = "name=Profiler";

    private String name = "Profiler Agent";
    private MBeanServer mBeanServer;
    private ObjectName profilerName;

    private JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    private JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();

    /**
     * Logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(YourKitProfilerAgent.class);

    /*
    * (non-Javadoc)
    *
    * @see org.mule.umo.manager.UMOAgent#getName()
    */
    public String getName()
    {
        return this.name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.manager.UMOAgent#setName(java.lang.String)
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.manager.UMOAgent#getDescription()
     */
    public String getDescription()
    {
        return "Profiler JMX Agent";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */
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

            UMOManagementContext managementContext = MuleServer.getManagementContext();
            profilerName = jmxSupport.getObjectName(jmxSupport.getDomainName(managementContext) + ":" + PROFILER_OBJECT_NAME);

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
            RegistryContext.getRegistry().unregisterAgent(this.getName());
        }
        catch (UMOException e)
        {
            // not interested, really
        }
    }

    private boolean isApiAvailable()
    {
        try{
            Class.forName("com.yourkit.api.Controller");
            return true;
        }
        catch(ClassNotFoundException e)
        {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.lifecycle.Startable#start()
     */
    public void start() throws UMOException
    {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.lifecycle.Stoppable#stop()
     */
    public void stop() throws UMOException
    {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.lifecycle.Disposable#dispose()
     */
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

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.manager.UMOAgent#registered()
     */
    public void registered()
    {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.manager.UMOAgent#unregistered()
     */
    public void unregistered()
    {
        // nothing to do
    }

}
