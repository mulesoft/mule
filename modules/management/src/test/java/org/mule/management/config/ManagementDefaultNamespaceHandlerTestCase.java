/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.config;

import org.mule.api.agent.Agent;
import org.mule.module.management.agent.JmxAgent;
import org.mule.module.management.agent.JmxServerNotificationAgent;
import org.mule.module.management.agent.Log4jAgent;
import org.mule.module.management.agent.Mx4jAgent;
import org.mule.module.management.support.SimplePasswordJmxAuthenticator;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ManagementDefaultNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "management-default-namespace-config.xml";
    }

    @Test
    public void testDefaultJmxAgentConfig() throws Exception
    {
        Agent agent = muleContext.getRegistry().lookupAgent("jmx-agent");
        assertNotNull(agent);
        assertEquals(JmxAgent.class, agent.getClass());
        JmxAgent jmxAgent = (JmxAgent) agent;
        
        assertEquals(false, jmxAgent.isCreateServer());
        assertEquals(true, jmxAgent.isLocateServer());
        assertEquals(true, jmxAgent.isEnableStatistics());
        assertEquals(SimplePasswordJmxAuthenticator.class, jmxAgent.getJmxAuthenticator().getClass());

        agent = muleContext.getRegistry().lookupAgent("jmx-log4j");
        assertNotNull(agent);
        assertEquals(Log4jAgent.class, agent.getClass());

        agent = muleContext.getRegistry().lookupAgent(JmxServerNotificationAgent.DEFAULT_AGENT_NAME);
        assertNotNull(agent);
        assertEquals(JmxServerNotificationAgent.class, agent.getClass());


        agent = muleContext.getRegistry().lookupAgent("jmx-mx4j-adaptor");
        assertNotNull(agent);
        assertEquals(Mx4jAgent.class, agent.getClass());

        
        agent = muleContext.getRegistry().lookupAgent("jmx-default-config");
        // see TODO in agent
//        assertNull(agent);
    }

}
