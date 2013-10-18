/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

