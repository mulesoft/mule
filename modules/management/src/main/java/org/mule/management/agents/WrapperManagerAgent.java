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
import org.mule.management.i18n.ManagementMessages;
import org.mule.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.management.support.JmxSupport;
import org.mule.management.support.JmxSupportFactory;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;

import java.util.List;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
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

    /**
     * For cases when Mule is embedded in another process and that external process
     * had registered the MBean.
     */
    public static final String DEFAULT_WRAPPER_MBEAN_NAME = "org.tanukisoftware.wrapper:type=WrapperManager";

    private static final Log logger = LogFactory.getLog(WrapperManagerAgent.class);

    /**
     * This property is set by the native launcher, used for extra checks.
     */
    private static final String WRAPPER_SYSTEM_PROPERTY_NAME = "wrapper.native_library";

    private MBeanServer mBeanServer;
    private ObjectName wrapperName;

    private JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    private JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();

    // atomic reference to avoid unnecessary construction calls
    private final AtomicReference/*<WrapperManagerMBean>*/ wrapperManagerRef = new AtomicReference();


    public WrapperManagerAgent()
    {
        super("Wrapper Manager");
    }

    /* @see org.mule.umo.lifecycle.Initialisable#initialise() */
    public void initialise() throws InitialisationException
    {

        try
        {
            final List servers = MBeanServerFactory.findMBeanServer(null);
            if (servers.isEmpty())
            {
                throw new InitialisationException(ManagementMessages.noMBeanServerAvailable(), this);
            }

            mBeanServer = (MBeanServer) servers.get(0);

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
            // Check if an external process registered a wrapper MBean under default name.
            else if (mBeanServer.isRegistered(jmxSupport.getObjectName(DEFAULT_WRAPPER_MBEAN_NAME)))
            {
                logger.info("Mule is embedded in a container already launched by a wrapper." +
                            "Duplicates will not be registered. Use the " + DEFAULT_WRAPPER_MBEAN_NAME + " MBean " +
                            "instead for control.");
                unregisterMeQuietly();
                return;
            }
            else
            {
                lazyInitWrapperManager();
                launchedByWrapper = ((WrapperManagerMBean) wrapperManagerRef.get()).isControlledByNativeWrapper();
            }

            if (!launchedByWrapper)
            {
                logger.info("This JVM hasn't been launched by the wrapper, the agent will not run.");
                unregisterMeQuietly();
                return;
            }

            wrapperName = jmxSupport.getObjectName(jmxSupport.getDomainName(managementContext) + ":" + WRAPPER_OBJECT_NAME);

            unregisterMBeansIfNecessary();
            mBeanServer.registerMBean(wrapperManagerRef.get(), wrapperName);
        }
        catch (InitialisationException iex)
        {
            // rethrow
            throw iex;
        }
        catch (Exception e)
        {
            throw new InitialisationException(
                CoreMessages.failedToStart("wrapper agent"), e, this);
        }
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

    protected void lazyInitWrapperManager() {
        WrapperManagerMBean wm = (WrapperManagerMBean) wrapperManagerRef.get();

        if (wm != null)
        {
            return;
        }

        wm = new WrapperManager();
        wrapperManagerRef.compareAndSet(null, wm);
    }

    /**
     * Unregister all MBeans if there are any left over the old deployment
     */
    protected void unregisterMBeansIfNecessary()
        throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException {
        if (mBeanServer == null || wrapperName == null)
        {
            return;
        }
        if (mBeanServer.isRegistered(wrapperName))
        {
            mBeanServer.unregisterMBean(wrapperName);
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
            managementContext.getRegistry().unregisterAgent(this.getName());
        }
        catch (UMOException e)
        {
            // not interested, really
        }
    }

}