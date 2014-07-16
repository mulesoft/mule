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
import org.mule.module.management.i18n.ManagementMessages;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tanukisoftware.wrapper.WrapperSystemPropertyUtil;
import org.tanukisoftware.wrapper.jmx.WrapperManager;
import org.tanukisoftware.wrapper.jmx.WrapperManagerMBean;
import org.tanukisoftware.wrapper.security.WrapperPermission;

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
    public static final String WRAPPER_JMX_NAME = "name=WrapperManager";

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
    private final AtomicReference<WrapperManagerMBean> wrapperManagerRef = new AtomicReference<WrapperManagerMBean>();


    public WrapperManagerAgent()
    {
        super("wrapper-manager");
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            List<?> servers = MBeanServerFactory.findMBeanServer(null);
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
                launchedByWrapper = wrapperManagerRef.get().isControlledByNativeWrapper();
            }

            if (!launchedByWrapper)
            {
                logger.info("This JVM hasn't been launched by the wrapper, the agent will not run.");
                unregisterMeQuietly();
                return;
            }

            final boolean containerMode = muleContext.getConfiguration().isContainerMode();
            if (containerMode)
            {
                // container mode, register mbean under Mule domain, no duplicates under each app's domain
                wrapperName = jmxSupport.getObjectName(JmxSupport.DEFAULT_JMX_DOMAIN_PREFIX + ":" + WRAPPER_JMX_NAME);
                if (mBeanServer.isRegistered(wrapperName))
                {
                    // ignore duplicate invocations when running in Mule container mode
                    return;
                }
            }
            else
            {
                // embedded case, use Mule's single domain
                wrapperName = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":" + WRAPPER_JMX_NAME);
            }

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
            throw new InitialisationException(CoreMessages.failedToStart("wrapper agent"), e, this);
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
        catch (Exception e)
        {
            logger.error("Couldn't unregister MBean: "
                         + (wrapperName != null ? wrapperName.getCanonicalName() : "null"), e);
        }
    }


    // /////////////////////////////////////////////////////////////////////////
    // Getters and setters
    // /////////////////////////////////////////////////////////////////////////

    @Override
    public String getDescription()
    {
        WrapperManagerMBean wm = wrapperManagerRef.get();
        if (wm == null)
        {
            return "Wrapper Manager";
        }
        else
        {
            return "Wrapper Manager: Mule PID #" + getJavaPID() + ", Wrapper PID #" + getWrapperPID();
        }
    }

    /**
     * This method is a copy of the implementation of
     * {@link WrapperManagerMBean#getJavaPID()} and it is here because that method is
     * not present in the {@link WrapperManagerMBean} until version 3.2.3.
     * SpringSource's TC Server uses The wrapper version 3.2.0 so having this method
     * here allows us to be compatible with TC Server.
     *
     * @return The PID of the Java process.
     * @see <a href="http://www.mulesoft.org/jira/browse/MULE-5106">MULE-5106</a>
     */
    public static int getJavaPID()
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new WrapperPermission("getJavaPID"));
        }

        return WrapperSystemPropertyUtil.getIntProperty("wrapper.java.pid", 0);
    }

    /**
     * This method is a copy of the implementation of
     * {@link WrapperManagerMBean#getWrapperPID()} and it is here because that method
     * is not present in the {@link WrapperManagerMBean} until version 3.2.3.
     * SpringSource's TC Server uses The wrapper version 3.2.0 so having this method
     * here allows us to be compatible with TC Server.
     *
     * @return The PID of the Wrapper process.
     * @see <a href="http://www.mulesoft.org/jira/browse/MULE-5106">MULE-5106</a>
     */
    public static int getWrapperPID()
    {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null)
        {
            sm.checkPermission(new WrapperPermission("getWrapperPID"));
        }

        return WrapperSystemPropertyUtil.getIntProperty("wrapper.pid", 0);
    }

    protected void lazyInitWrapperManager()
    {
        WrapperManagerMBean wm = wrapperManagerRef.get();

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
            throws MalformedObjectNameException, InstanceNotFoundException, MBeanRegistrationException
    {
        if (mBeanServer == null || wrapperName == null)
        {
            return;
        }
        if (mBeanServer.isRegistered(wrapperName))
        {
            mBeanServer.unregisterMBean(wrapperName);
        }
    }

}
