/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management;

import org.mule.module.management.agent.RmiRegistryAgent;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;

import org.junit.Test;

/**
 * This base test case will create a new <code>MBean Server</code> if necessary,
 * and will clean up any registered MBeans in its <code>tearDown()</code> method.
 */
public abstract class AbstractMuleJmxTestCase extends AbstractMuleContextTestCase
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

        mBeanServer = ManagementFactory.getPlatformMBeanServer();

    }

    protected void unregisterMBeansByMask(final String mask) throws Exception
    {
        Set<ObjectInstance> objectInstances = mBeanServer.queryMBeans(jmxSupport.getObjectName(mask), null);
        for (ObjectInstance instance : objectInstances)
        {
            try
            {
                mBeanServer.unregisterMBean(instance.getObjectName());
            }
            catch (Exception e)
            {
                // ignore
            }
        }
    }

    protected void doTearDown() throws Exception
    {
        String domainName = jmxSupport.getDomainName(muleContext);
        unregisterMBeansByMask(domainName + ":*");
        unregisterMBeansByMask(domainName + ".1:*");
        unregisterMBeansByMask(domainName + ".2:*");
        mBeanServer = null;
    }

    @Test
    public void testDummy()
    {
        // this method only exists to silence the test runner
    }

}
