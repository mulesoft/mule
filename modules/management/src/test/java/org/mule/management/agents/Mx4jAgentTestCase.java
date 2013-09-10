/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
