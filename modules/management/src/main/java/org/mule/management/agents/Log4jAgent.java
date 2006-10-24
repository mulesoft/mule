/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.apache.log4j.jmx.HierarchyDynamicMBean;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import java.util.Iterator;
import java.util.Set;

/**
 * <code>Log4jAgent</code> exposes the configuration of the Log4J instance running
 * in Mule for Jmx management
 */
public class Log4jAgent implements UMOAgent
{

    private String name = "Log4j Agent";
    private MBeanServer mBeanServer;
    public static final String JMX_OBJECT_NAME = "log4j:type=Hierarchy";

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
        return "Log4j JMX Agent";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */
    public void initialise() throws InitialisationException
    {
        try
        {
            mBeanServer = (MBeanServer)MBeanServerFactory.findMBeanServer(null).get(0);
            final ObjectName objectName = ObjectName.getInstance(JMX_OBJECT_NAME);
            // unregister existing Log4j MBean first if required
            unregisterMBeansIfNecessary();
            mBeanServer.registerMBean(new HierarchyDynamicMBean(), objectName);
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_TO_START_X, "JMX Agent"), e, this);
        }
    }

    /**
     * Unregister all log4j MBeans if there are any left over the old deployment
     */
    protected void unregisterMBeansIfNecessary()
        throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException
    {
        if (mBeanServer.isRegistered(ObjectName.getInstance(JMX_OBJECT_NAME)))
        {
            // unregister all log4jMBeans and loggers
            Set log4jMBeans = mBeanServer.queryMBeans(ObjectName.getInstance("log4j*:*"), null);
            for (Iterator it = log4jMBeans.iterator(); it.hasNext();)
            {
                ObjectInstance objectInstance = (ObjectInstance)it.next();
                ObjectName theName = objectInstance.getObjectName();
                mBeanServer.unregisterMBean(theName);
            }
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
        // nothing to do
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
