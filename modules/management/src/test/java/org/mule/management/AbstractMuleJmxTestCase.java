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

import org.mule.module.management.agent.RmiRegistryAgent;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;
import org.mule.tck.AbstractMuleTestCase;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;

/**
 * This base test case will create a new <code>MBean Server</code> if necessary,
 * and will clean up any registered MBeans in its <code>tearDown()</code> method.
 */
public abstract class AbstractMuleJmxTestCase extends AbstractMuleTestCase
{
    protected MBeanServer mBeanServer;
    protected JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    protected JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport(); 

    protected void doSetUp() throws Exception
    {
        RmiRegistryAgent rmiRegistryAgent = new RmiRegistryAgent();
        rmiRegistryAgent.setMuleContext(muleContext);
        rmiRegistryAgent.initialise();
        muleContext.getRegistry().registerAgent(rmiRegistryAgent);
        
        // simulate a running environment with Log4j MBean already registered
        List servers = MBeanServerFactory.findMBeanServer(null);
        if (servers.size() == 0)
        {
            MBeanServerFactory.createMBeanServer();
        }

        mBeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
    }

    protected void unregisterMBeansByMask(final String mask) throws Exception
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
        // Don't unregister MBean's here as ManamagmentContext disposal disposes agents which unregister
        // their MBeans and give errors if they can't find the MBeans they registered.
        // Any MBean's that are registered manually in TestCase should be unregistered in the same test case.

        // Release MBeanServer so MBeanServer instance can't get passed over from one
        // test to another in same circumstances.
        MBeanServerFactory.releaseMBeanServer(mBeanServer);
        mBeanServer = null;
    }

    public void testDummy()
    {
        // this method only exists to silence the test runner
    }

}
