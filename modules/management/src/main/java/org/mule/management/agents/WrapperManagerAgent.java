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

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.AbstractAgent;
import org.mule.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.management.support.JmxSupport;
import org.mule.management.support.JmxSupportFactory;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tanukisoftware.wrapper.jmx.WrapperManager;
import org.tanukisoftware.wrapper.jmx.WrapperManagerMBean;

/**
 * This agent integrates Java Service Wrapper into Mule. See
 * <a href="http://wrapper.tanukisoftware.org">http://wrapper.tanukisoftware.org</a>
 * for more details.
 */
public class WrapperManagerAgent extends AbstractAgent
{
    /**
     * MBean name to register under.
     */
    public static final String WRAPPER_OBJECT_NAME = "name=WrapperManager";

    private static final Log logger = LogFactory.getLog(WrapperManagerAgent.class);

    /**
     * This property is set by the native launcher, used for extra checks.
     */
    private static final String WRAPPER_SYSTEM_PROPERTY_NAME = "wrapper.native_library";

    private MBeanServer mBeanServer;
    private ObjectName wrapperName;

    private JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    private JmxSupport jmxSupport;

    // atomic reference to avoid unnecessary construction calls
    private final AtomicReference/*<WrapperManagerMBean>*/ wrapperManagerRef = new AtomicReference();


    public WrapperManagerAgent()
    {
        super("Wrapper Manager");
    }

    /* @see org.mule.umo.lifecycle.Initialisable#initialise() */
    public void initialise() throws InitialisationException
    {

        /*
           Perform an extra check ourselves. If 'wrapper.native_library' property has
           not been set, which is the case for embedded scenarios, don't even try to
           construct the wrapper manager bean, as it performs a number of checks internally
           and outputs a very verbose warning.
         */
        boolean launchedByWrapper;
        if (System.getProperty(WRAPPER_SYSTEM_PROPERTY_NAME) == null)
        {
            launchedByWrapper = false;
        }
        else
        {
            lazyInitWrapperManager();
            launchedByWrapper = ((WrapperManagerMBean) wrapperManagerRef.get()).isControlledByNativeWrapper();
        }

        if (!launchedByWrapper)
        {
            logger.info("This JVM hasn't been launched by the wrapper, the agent will not run.");
            try
            {
                // remove the agent from the list, it's not functional
                managementContext.getRegistry().unregisterAgent(this.getName());
            }
            catch (UMOException e) {
                // not interested, really
            }
            return;
        }


        jmxSupport = jmxSupportFactory.getJmxSupport();
        final List servers = MBeanServerFactory.findMBeanServer(null);
        if (servers.isEmpty())
        {
            // TODO construct proper exception
            throw new RuntimeException("no mbean servers found");
        }

        try
        {
            mBeanServer = (MBeanServer) servers.get(0);

            wrapperName = jmxSupport.getObjectName(jmxSupport.getDomainName(managementContext) + ":" + WRAPPER_OBJECT_NAME);

            unregisterMBeansIfNecessary();
            mBeanServer.registerMBean(wrapperManagerRef.get(), wrapperName);
        }

        catch (Exception e)
        {
            throw new InitialisationException(
                CoreMessages.failedToStart("wrapper agent"), e, this);
        }
    }

    protected void lazyInitWrapperManager() {
        WrapperManagerMBean wm = (WrapperManagerMBean) wrapperManagerRef.get();

        if (wm != null)
        {
            return;
        }

        wm = new WrapperManager();
        wrapperManagerRef.compareAndSet(null, wm);
    }

    /* @see org.mule.umo.lifecycle.Startable#start() */
    public void start() throws UMOException {
        // no-op
    }

    /* @see org.mule.umo.lifecycle.Stoppable#stop() */
    public void stop() throws UMOException
    {
        // no-op
    }

    /**
     * Unregister all MBeans if there are any left over the old deployment
     */
    protected void unregisterMBeansIfNecessary()
        throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException {
        if (mBeanServer == null || wrapperName != null)
        {
            return;
        }
        if (mBeanServer.isRegistered(wrapperName))
        {
            mBeanServer.unregisterMBean(wrapperName);
        }
    }

    /* @see org.mule.umo.lifecycle.Disposable#dispose() */
    public void dispose()
    {
        try
        {
            unregisterMBeansIfNecessary();
        }
        catch (Exception e)
        {
            logger.error("Couldn't unregister MBean: "
                         + (wrapperName != null ? wrapperName.getCanonicalName() : "null"), e);
        }
    }

    /* @see org.mule.umo.manager.UMOAgent#registered() */
    public void registered()
    {
        // nothing to do
    }

    /* @see org.mule.umo.manager.UMOAgent#unregistered() */
    public void unregistered()
    {
        // nothing to do
    }

    // /////////////////////////////////////////////////////////////////////////
    // Getters and setters
    // /////////////////////////////////////////////////////////////////////////

    /* @see org.mule.umo.manager.UMOAgent#getDescription() */
    public String getDescription()
    {
        WrapperManagerMBean wm = (WrapperManagerMBean) wrapperManagerRef.get();
        if (wm == null)
        {
            return "Wrapper Manager";
        }
        else return "Wrapper Manager: Mule PID #" + wm.getJavaPID() +
                ", Wrapper PID #" + wm.getWrapperPID();
    }
}