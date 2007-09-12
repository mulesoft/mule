/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management;

import org.mule.management.agents.RmiRegistryAgent;
import org.mule.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.management.support.JmxSupport;
import org.mule.management.support.JmxSupportFactory;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.RegistryContext;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

/**
 * This base test case will create a new <code>MBean Server</code> if necessary,
 * and will clean up any registered MBeans in its <code>tearDown()</code> method.
 */
public class AbstractMuleJmxTestCase extends AbstractMuleTestCase
{
    protected MBeanServer mBeanServer;
    protected JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    protected JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport(); 

    protected void doSetUp() throws Exception
    {
        RmiRegistryAgent rmiRegistryAgent = new RmiRegistryAgent();
        RegistryContext.getRegistry().registerAgent(rmiRegistryAgent, managementContext);
        
        // simulate a running environment with Log4j MBean already registered
        List servers = MBeanServerFactory.findMBeanServer(null);
        if (servers.size() == 0)
        {
            MBeanServerFactory.createMBeanServer();
        }

        mBeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
    }

    private void unregisterMBeansByMask(final String mask) throws Exception
    {
        Set objectInstances = mBeanServer.queryMBeans(jmxSupport.getObjectName(mask), null);
        for (Iterator it = objectInstances.iterator(); it.hasNext();)
        {
            ObjectInstance instance = (ObjectInstance) it.next();
            mBeanServer.unregisterMBean(instance.getObjectName());
        }
    }

    protected void doTearDown() throws Exception
    {
        unregisterMBeansByMask("*.*:*");
        unregisterMBeansByMask("log4j:*");
        mBeanServer = null;
    }

    public void testDummy()
    {
        // this method only exists to silence the test runner
    }

}
