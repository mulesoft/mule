/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.agents;

import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.module.management.agent.Log4jAgent;

import javax.management.ObjectName;

import org.apache.log4j.jmx.HierarchyDynamicMBean;
import org.junit.Test;

public class Log4jAgentTestCase extends AbstractMuleJmxTestCase
{
    @Test
    public void testRedeploy() throws Exception
    {
        mBeanServer.registerMBean(new HierarchyDynamicMBean(),
                                  ObjectName.getInstance(Log4jAgent.JMX_OBJECT_NAME));

        Log4jAgent agent = new Log4jAgent();
        agent.initialise();
    }
    
    protected void doTearDown() throws Exception
    {
        // This MBean was registered manually so needs to be unregistered manually in tearDown()
        unregisterMBeansByMask(Log4jAgent.JMX_OBJECT_NAME);
        super.doTearDown();
    }
}
