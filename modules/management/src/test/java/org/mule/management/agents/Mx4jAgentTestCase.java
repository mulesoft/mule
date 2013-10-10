/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.agents;

import org.mule.management.AbstractMuleJmxTestCase;
import org.mule.module.management.agent.Mx4jAgent;

import mx4j.tools.adaptor.http.HttpAdaptor;
import org.junit.Test;

/**
 * Test that the lifecycle is properly managed.
 */
public class Mx4jAgentTestCase extends AbstractMuleJmxTestCase
{
    @Test
    public void testRedeploy() throws Exception
    {
        final String name = jmxSupport.getDomainName(muleContext) +
                            ":" + Mx4jAgent.HTTP_ADAPTER_OBJECT_NAME;
        mBeanServer.registerMBean(new HttpAdaptor(), jmxSupport.getObjectName(name));

        Mx4jAgent agent = new Mx4jAgent();
        agent.setMuleContext(muleContext);
        agent.initialise();
    }
}
