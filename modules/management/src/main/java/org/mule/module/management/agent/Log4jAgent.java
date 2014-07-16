/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;

import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.log4j.jmx.HierarchyDynamicMBean;

/**
 * <code>Log4jAgent</code> exposes the configuration of the Log4J instance running
 * in Mule for Jmx management
 */
public class Log4jAgent extends AbstractAgent
{
    private MBeanServer mBeanServer;
    public static final String JMX_OBJECT_NAME = "log4j:type=Hierarchy";

    private JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    private JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();

    public Log4jAgent()
    {
        super("jmx-log4j");
    }

    @Override
    public String getDescription() {
        return "JMX Log4J Agent";
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
            final ObjectName objectName = jmxSupport.getObjectName(JMX_OBJECT_NAME);
            // unregister existing Log4j MBean first if required
            unregisterMBeansIfNecessary();
            mBeanServer.registerMBean(new HierarchyDynamicMBean(), objectName);
        }
        catch (Exception e)
        {
            throw new InitialisationException(CoreMessages.failedToStart("Log4j Agent"), e, this);
        }
    }

    /**
     * Unregister log4j MBeans if there are any left over the old deployment
     */
    protected void unregisterMBeansIfNecessary()
        throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException
    {
        if (mBeanServer.isRegistered(jmxSupport.getObjectName(JMX_OBJECT_NAME)))
        {
            // unregister all log4jMBeans and loggers
            Set<ObjectInstance> log4jMBeans = mBeanServer.queryMBeans(jmxSupport.getObjectName("log4j*:*"), null);
            for (ObjectInstance objectInstance : log4jMBeans)
            {
                ObjectName theName = objectInstance.getObjectName();
                if (mBeanServer.isRegistered(theName))
                {
                    mBeanServer.unregisterMBean(theName);
                }
            }
        }
    }

    @Override
    public void start() throws MuleException
    {
        // nothing to do
    }

    @Override
    public void stop() throws MuleException
    {
        // nothing to do
    }

    @Override
    public void dispose()
    {
        try
        {
            unregisterMBeansIfNecessary();
        }
        catch (Exception ex)
        {
            // ignore
        }
    }
}
