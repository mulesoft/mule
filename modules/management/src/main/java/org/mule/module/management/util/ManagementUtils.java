/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.util;

import org.mule.module.management.agent.WrapperManagerAgent;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;
import org.tanukisoftware.wrapper.jmx.WrapperManager;
import org.tanukisoftware.wrapper.jmx.WrapperManagerMBean;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class ManagementUtils
{

    protected static JmxSupportFactory jmxSupportFactory;
    protected static JmxSupport jmxSupport;

    public static void restart() throws Exception
    {
        WrapperManagerMBean proxy = getProxy();
        if (proxy != null) {
            proxy.restart();
        }
        else
        {
            throw new RuntimeException("The wrapper is not enabled.");
        }
    }


    protected synchronized static WrapperManagerMBean getProxy() throws MalformedObjectNameException, MBeanRegistrationException, InstanceAlreadyExistsException, NotCompliantMBeanException
    {
        if (jmxSupport == null)
        {
            jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
            jmxSupport = jmxSupportFactory.getJmxSupport();
        }

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final String jmxNameForMule = String.format("%s:%s", JmxSupport.DEFAULT_JMX_DOMAIN_PREFIX, WrapperManagerAgent.WRAPPER_JMX_NAME);
        ObjectName on = jmxSupport.getObjectName(jmxNameForMule);
        if (!mBeanServer.isRegistered(on))
        {
            mBeanServer.registerMBean(new WrapperManager(), on);
        }

        WrapperManagerMBean proxy = MBeanServerInvocationHandler.newProxyInstance(
            mBeanServer, on, WrapperManagerMBean.class, false);
        return proxy;
    }

}

